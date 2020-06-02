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

import { isEmpty } from 'lodash';
import { denormalize } from 'normalizr';
import { ofType } from 'redux-observable';
import { of } from 'rxjs';
import {
  catchError,
  concatAll,
  exhaustMap,
  withLatestFrom,
  takeUntil,
  takeWhile,
  concatMap,
} from 'rxjs/operators';

import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import { LOG_LEVEL } from 'const';
import * as schema from 'store/schema';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { updateWorkerAndWorkspace$ } from '../worker/updateWorkerEpic';
import { updateBrokerAndWorkspace$ } from '../broker/updateBrokerEpic';
import { updateZookeeperAndWorkspace$ } from '../zookeeper/updateZookeeperEpic';
import { stopWorker$ } from '../worker/stopWorkerEpic';
import { stopTopic$ } from '../topic/stopTopicEpic';
import { stopBroker$ } from '../broker/stopBrokerEpic';
import { stopZookeeper$ } from '../zookeeper/stopZookeeperEpic';
import { startWorker$ } from '../worker/startWorkerEpic';
import { startTopic$ } from '../topic/startTopicEpic';
import { startBroker$ } from '../broker/startBrokerEpic';
import { startZookeeper$ } from '../zookeeper/startZookeeperEpic';

const finalize$ = ({ workerKey, workspaceKey }) =>
  of(
    of(actions.restartWorkspace.success()),
    of(
      actions.createEventLog.trigger({
        title: `Successfully Restart workspace ${workspaceKey.name}.`,
        type: LOG_LEVEL.info,
      }),
    ),
    // Refetch node list after creation successfully in order to get the runtime data
    of(actions.fetchNodes.trigger()),
    // Refetch connector list (inspect worker and shabondi) for those new added plugins
    of(actions.fetchWorker.trigger(workerKey)),
  ).pipe(concatAll());

const isRunning = (state$) => (key, schema) => {
  const data = denormalize(getId(key), schema, state$.value?.entities);
  return !isEmpty(data) && data.state === SERVICE_STATE.RUNNING;
};

export default (action$, state$) =>
  action$.pipe(
    ofType(actions.restartWorkspace.TRIGGER),
    withLatestFrom(state$),
    exhaustMap(([action]) => {
      const {
        workspaceKey,
        zookeeperKey,
        brokerKey,
        workerKey,
        workerSettings = {},
        brokerSettings = {},
        zookeeperSettings = {},
        topics = [],
      } = action.payload.values;

      const isServiceRunning = isRunning(state$);

      return of(
        stopWorker$(workerKey).pipe(
          takeWhile(() => isServiceRunning(workerKey, schema.worker)),
        ),
        updateWorkerAndWorkspace$({
          workspaceKey,
          ...workerKey,
          ...workerSettings,
        }),

        of(...topics).pipe(
          concatMap((topicKey) =>
            stopTopic$(topicKey).pipe(
              takeWhile(() => isServiceRunning(topicKey, schema.topic)),
            ),
          ),
        ),

        stopBroker$(brokerKey).pipe(
          takeWhile(() => isServiceRunning(brokerKey, schema.broker)),
        ),
        updateBrokerAndWorkspace$({
          workspaceKey,
          ...brokerKey,
          ...brokerSettings,
        }),

        stopZookeeper$(zookeeperKey).pipe(
          takeWhile(() => isServiceRunning(zookeeperKey, schema.zookeeper)),
        ),
        updateZookeeperAndWorkspace$({
          workspaceKey,
          ...zookeeperKey,
          ...zookeeperSettings,
        }),

        startZookeeper$(zookeeperKey),

        startBroker$(brokerKey),

        of(...topics).pipe(concatMap((topicKey) => startTopic$(topicKey))),

        startWorker$(workerKey),

        finalize$({ workerKey, workspaceKey }),
      ).pipe(
        concatAll(),
        catchError((error) => of(actions.restartWorkspace.failure(error))),
        takeUntil(action$.pipe(ofType(actions.pauseRestartWorkspace.TRIGGER))),
      );
    }),
  );
