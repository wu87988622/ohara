/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { isEmpty, values } from 'lodash';
import { denormalize } from 'normalizr';
import { ofType } from 'redux-observable';
import { defer, of, from } from 'rxjs';
import {
  catchError,
  concatAll,
  exhaustMap,
  map,
  startWith,
  withLatestFrom,
  takeUntil,
  takeWhile,
  concatMap,
  mergeMap,
} from 'rxjs/operators';

import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import * as workspaceApi from 'api/workspaceApi';
import { LOG_LEVEL } from 'const';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId, getKey } from 'utils/object';
import { stopWorker$ } from '../worker/stopWorkerEpic';
import { stopBroker$ } from '../broker/stopBrokerEpic';
import { stopZookeeper$ } from '../zookeeper/stopZookeeperEpic';
import { deleteWorker$ } from '../worker/deleteWorkerEpic';
import { deleteBroker$ } from '../broker/deleteBrokerEpic';
import { deleteZookeeper$ } from '../zookeeper/deleteZookeeperEpic';
import { stopTopic$ } from '../topic/stopTopicEpic';
import { deleteTopic$ } from '../topic/deleteTopicEpic';
import { deleteFile$ } from '../file/deleteFileEpic';
import { deletePipeline$ } from '../pipeline/deletePipelineEpic';
import { deleteConnector$ } from '../connector/deleteConnectorEpic';
import { deleteStream$ } from '../stream/deleteStreamEpic';
import { deleteShabondi$ } from '../shabondi/deleteShabondiEpic';

const deleteWorkspace$ = (params) => {
  const workspaceId = getId(params);
  return defer(() => workspaceApi.remove(params)).pipe(
    map(() => actions.deleteWorkspace.success({ workspaceId })),
    startWith(actions.deleteWorkspace.request({ workspaceId })),
  );
};

const isRunning = (state$) => (key, schema) => {
  const data = denormalize(getId(key), schema, state$.value?.entities);
  return !isEmpty(data) && data.state === SERVICE_STATE.RUNNING;
};

const isOrphanObject = (state$) => (key, schema) => {
  const data = denormalize(getId(key), schema, state$.value?.entities);
  return !isEmpty(data) && !data.state;
};

const isNonExistedObject = (state$) => (key, schema) => {
  const data = denormalize(getId(key), schema, state$.value?.entities);
  return isEmpty(data);
};

const finalize$ = (options) =>
  of(options).pipe(
    map(() => {
      if (options?.onSuccess) {
        options.onSuccess();
      }
    }),
    concatMap(() =>
      from([
        actions.closeDeleteWorkspace.trigger(),
        actions.initializeApp.trigger({}),
      ]),
    ),
  );

export default (action$, state$) =>
  action$.pipe(
    ofType(actions.deleteWorkspace.TRIGGER),
    withLatestFrom(state$),
    exhaustMap(([action]) => {
      const {
        workspaceKey,
        zookeeperKey,
        brokerKey,
        workerKey,
        pipelineKeys,
        connectorKeys,
        shabondiKeys,
        streamKeys,
        files,
      } = action.payload.values;
      const options = action.payload.options;

      const isServiceRunning = isRunning(state$);
      const isServiceOrphan = isOrphanObject(state$);
      const isServiceNonExisted = isNonExistedObject(state$);

      return of(
        of(
          of(...connectorKeys).pipe(
            mergeMap((connectorKey) =>
              deleteConnector$({ params: connectorKey }),
            ),
          ),
          of(...streamKeys).pipe(
            mergeMap((streamKey) => deleteStream$({ params: streamKey })),
          ),
          of(...shabondiKeys).pipe(
            mergeMap((shabondiKey) => deleteShabondi$({ params: shabondiKey })),
          ),
          of(...pipelineKeys).pipe(
            mergeMap((pipelineKey) => deletePipeline$(pipelineKey)),
          ),
        ).pipe(concatAll()),

        stopWorker$(workerKey).pipe(
          takeWhile(() => isServiceRunning(workerKey, schema.worker)),
        ),

        of(
          from(
            values(state$.value?.entities?.topics).map((topic) =>
              getKey(topic),
            ),
          ).pipe(
            concatMap((params) =>
              stopTopic$(params).pipe(
                takeWhile(() => isServiceRunning(params, schema.topic)),
              ),
            ),
          ),
          stopBroker$(brokerKey),
        ).pipe(
          concatAll(),
          takeWhile(
            () =>
              !isServiceRunning(workerKey, schema.worker) &&
              isServiceRunning(brokerKey, schema.broker),
          ),
        ),

        stopZookeeper$(zookeeperKey).pipe(
          takeWhile(
            () =>
              !isServiceRunning(brokerKey, schema.broker) &&
              isServiceRunning(zookeeperKey, schema.zookeeper),
          ),
        ),

        deleteWorker$(workerKey).pipe(
          takeWhile(() => isServiceOrphan(workerKey, schema.worker)),
        ),

        of(
          from(
            values(state$.value?.entities?.topics).map((topic) =>
              getKey(topic),
            ),
          ).pipe(
            concatMap((params) =>
              deleteTopic$(params).pipe(
                takeWhile(() => isServiceOrphan(params, schema.topic)),
              ),
            ),
          ),
          deleteBroker$(brokerKey),
        ).pipe(
          concatAll(),
          takeWhile(() => isServiceOrphan(brokerKey, schema.broker)),
        ),

        deleteZookeeper$(zookeeperKey).pipe(
          takeWhile(() => isServiceOrphan(zookeeperKey, schema.zookeeper)),
        ),

        //TODO : Add rollback part logic in #4898
        // create -> start

        of(
          deleteWorkspace$(workspaceKey),
          of(...files).pipe(
            mergeMap((file) =>
              deleteFile$({ name: file.name, group: file.group }),
            ),
          ),
          of(
            actions.createEventLog.trigger({
              title: `Successfully deleted workspace ${workspaceKey.name}.`,
              type: LOG_LEVEL.info,
            }),
          ),
        ).pipe(
          concatAll(),
          takeWhile(
            () =>
              isServiceNonExisted(workerKey, schema.worker) &&
              isServiceNonExisted(brokerKey, schema.broker) &&
              isServiceNonExisted(zookeeperKey, schema.zookeeper),
          ),
        ),

        finalize$(options),
      ).pipe(
        concatAll(),
        catchError((err) => {
          if (options?.onError) {
            options.onError(err);
          }

          return from([
            actions.deleteWorkspace.failure(err),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]);
        }),
        takeUntil(action$.pipe(ofType(actions.pauseDeleteWorkspace.TRIGGER))),
      );
    }),
  );
