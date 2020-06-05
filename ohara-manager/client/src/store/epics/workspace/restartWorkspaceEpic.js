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
import { ofType } from 'redux-observable';
import { of, defer } from 'rxjs';
import {
  catchError,
  concatAll,
  exhaustMap,
  withLatestFrom,
  takeUntil,
  takeWhile,
  concatMap,
  map,
} from 'rxjs/operators';

import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import { LOG_LEVEL, KIND } from 'const';
import * as actions from 'store/actions';
import * as workerApi from 'api/workerApi';
import * as topicApi from 'api/topicApi';
import * as brokerApi from 'api/brokerApi';
import * as zookeeperApi from 'api/zookeeperApi';
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

const finalize$ = ({ zookeeperKey, brokerKey, workerKey, workspaceKey }) =>
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
    // Refetch broker to update broker info
    of(actions.fetchBroker.trigger(brokerKey)),
    // Refetch zookeeper to update zookeeper info
    of(actions.fetchZookeeper.trigger(zookeeperKey)),
  ).pipe(concatAll());

const isServiceRunning$ = async (api) => {
  const isRunning = await defer(() => api)
    .pipe(
      map((res) => res.data),
      map((data) => !isEmpty(data) && data?.state === SERVICE_STATE.RUNNING),
    )
    .toPromise();
  return isRunning;
};

const setTargetService$ = (targetService) => (service) => {
  let tmpTargetService = [];
  switch (targetService) {
    case KIND.broker:
      tmpTargetService = [KIND.broker, KIND.worker];
      break;
    case KIND.worker:
      tmpTargetService = [KIND.worker];
      break;
    default:
      tmpTargetService = [KIND.zookeeper, KIND.broker, KIND.worker];
  }
  return tmpTargetService.includes(service);
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
      const targetService = action.payload.option;
      const isTarget = setTargetService$(targetService);

      return of(
        stopWorker$(workerKey).pipe(
          takeWhile(() => isTarget(KIND.worker)),
          takeWhile(
            async () => await isServiceRunning$(workerApi.get(workerKey)),
          ),
        ),

        updateWorkerAndWorkspace$({
          workspaceKey,
          ...workerKey,
          ...workerSettings,
        }).pipe(
          takeWhile(() => isTarget(KIND.worker)),
          takeWhile(
            async () => await !isServiceRunning$(workerApi.get(workerKey)),
          ),
        ),

        of(...topics).pipe(
          concatMap((topicKey) =>
            stopTopic$(topicKey).pipe(
              takeWhile(() => isTarget(KIND.broker)),
              takeWhile(
                async () => await isServiceRunning$(topicApi.get(topicKey)),
              ),
            ),
          ),
        ),

        stopBroker$(brokerKey).pipe(
          takeWhile(() => isTarget(KIND.broker)),
          takeWhile(
            async () => await isServiceRunning$(brokerApi.get(brokerKey)),
          ),
        ),
        updateBrokerAndWorkspace$({
          workspaceKey,
          ...brokerKey,
          ...brokerSettings,
        }).pipe(
          takeWhile(() => isTarget(KIND.broker)),
          takeWhile(
            async () => await !isServiceRunning$(brokerApi.get(brokerKey)),
          ),
        ),

        stopZookeeper$(zookeeperKey).pipe(
          takeWhile(() => isTarget(KIND.zookeeper)),
          takeWhile(
            async () => await isServiceRunning$(zookeeperApi.get(zookeeperKey)),
          ),
        ),
        updateZookeeperAndWorkspace$({
          workspaceKey,
          ...zookeeperKey,
          ...zookeeperSettings,
        }).pipe(
          takeWhile(() => isTarget(KIND.zookeeper)),
          takeWhile(
            async () =>
              await !isServiceRunning$(zookeeperApi.get(zookeeperKey)),
          ),
        ),

        startZookeeper$(zookeeperKey).pipe(
          takeWhile(() => isTarget(KIND.zookeeper)),
          takeWhile(
            async () =>
              await !isServiceRunning$(zookeeperApi.get(zookeeperKey)),
          ),
        ),

        startBroker$(brokerKey).pipe(
          takeWhile(() => isTarget(KIND.broker)),
          takeWhile(
            async () => await !isServiceRunning$(brokerApi.get(brokerKey)),
          ),
        ),

        of(...topics).pipe(
          concatMap((topicKey) =>
            startTopic$(topicKey).pipe(
              takeWhile(() => isTarget(KIND.broker)),
              takeWhile(
                async () => await !isServiceRunning$(topicApi.get(topicKey)),
              ),
            ),
          ),
        ),

        startWorker$(workerKey).pipe(
          takeWhile(() => isTarget(KIND.worker)),
          takeWhile(
            async () => await !isServiceRunning$(workerApi.get(workerKey)),
          ),
        ),

        finalize$({ zookeeperKey, brokerKey, workerKey, workspaceKey }),
      ).pipe(
        concatAll(),
        catchError((error) => of(actions.restartWorkspace.failure(error))),
        takeUntil(action$.pipe(ofType(actions.pauseRestartWorkspace.TRIGGER))),
      );
    }),
  );
