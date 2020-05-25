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

import { normalize } from 'normalizr';
import { merge } from 'lodash';
import { ofType } from 'redux-observable';
import { defer, of, from } from 'rxjs';
import {
  catchError,
  concatAll,
  delay,
  exhaustMap,
  map,
  retryWhen,
  startWith,
  take,
  withLatestFrom,
  takeUntil,
  takeWhile,
  mergeMap,
  switchMap,
} from 'rxjs/operators';

import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import * as brokerApi from 'api/brokerApi';
import * as workerApi from 'api/workerApi';
import * as workspaceApi from 'api/workspaceApi';
import * as zookeeperApi from 'api/zookeeperApi';
import * as topicApi from 'api/topicApi';
import { ACTIONS } from 'const';
import { LOG_LEVEL } from 'const';
import * as schema from 'store/schema';
import * as actions from 'store/actions';
import { getId } from 'utils/object';

const duration = 1000;
const retry = 10;

const updateZookeeper$ = (values, isRollback) =>
  of(
    defer(() =>
      zookeeperApi.update({
        ...values.settings,
        name: values.objectKey.name,
        group: values.objectKey.group,
        tags: values.tmp,
      }),
    ).pipe(
      delay(duration),
      map(res => res.data),
      map(data => normalize(data, schema.zookeeper)),
      map(normalizedData =>
        merge(normalizedData, { zookeeperId: getId(values.objectKey) }),
      ),
      map(normalizedData => actions.updateZookeeper.success(normalizedData)),
      startWith(actions.updateZookeeper.request()),
    ),
  ).pipe(
    takeWhile(() => !isRollback),
    concatAll(),
  );

const updateBroker$ = (values, isRollback) =>
  of(
    defer(() =>
      brokerApi.update({
        ...values.settings,
        name: values.objectKey.name,
        group: values.objectKey.group,
        tags: values.tmp,
      }),
    ).pipe(
      delay(duration),
      map(res => res.data),
      map(data => normalize(data, schema.broker)),
      map(normalizedData =>
        merge(normalizedData, { brokerId: getId(values.objectKey) }),
      ),
      map(normalizedData => actions.updateBroker.success(normalizedData)),
      startWith(actions.updateBroker.request()),
    ),
  ).pipe(
    takeWhile(() => !isRollback),
    concatAll(),
  );

const updateWorker$ = (values, isRollback) =>
  of(
    defer(() =>
      workerApi.update({
        ...values.settings,
        name: values.objectKey.name,
        group: values.objectKey.group,
        tags: values.tmp,
      }),
    ).pipe(
      delay(duration),
      map(res => res.data),
      map(data => normalize(data, schema.worker)),
      map(normalizedData =>
        merge(normalizedData, { workerId: getId(values.objectKey) }),
      ),
      map(normalizedData => actions.updateWorker.success(normalizedData)),
      startWith(actions.updateWorker.request()),
    ),
  ).pipe(
    takeWhile(() => !isRollback),
    concatAll(),
  );

const stopZookeeper$ = params =>
  of(
    defer(() => zookeeperApi.stop(params)).pipe(
      delay(duration),
      map(() => actions.stopZookeeper.request()),
      catchError(error => of(actions.stopZookeeper.failure(error))),
    ),
  ).pipe(concatAll());

const stopBroker$ = params =>
  of(
    defer(() => brokerApi.stop(params)).pipe(
      delay(duration),
      map(() => actions.stopBroker.request()),
      catchError(error => of(actions.stopBroker.failure(error))),
    ),
  ).pipe(concatAll());

const stopWorker$ = params =>
  of(
    defer(() => workerApi.stop(params)).pipe(
      delay(duration),
      map(() => actions.stopWorker.request()),
      catchError(error => of(actions.stopWorker.failure(error))),
    ),
  ).pipe(concatAll());

const stopTopic$ = topic =>
  of(
    defer(() => topicApi.stop(topic)).pipe(
      delay(duration),
      map(() => actions.stopTopic.request()),
      catchError(error => of(actions.stopTopic.failure(error))),
    ),
  ).pipe(concatAll());

const stopTopics$ = topics =>
  of(...topics).pipe(mergeMap(topics => stopTopic$(topics)));

const startZookeeper$ = (params, skipList) =>
  of(
    defer(() => zookeeperApi.start(params)).pipe(
      delay(duration),
      map(() => actions.startZookeeper.request()),
      catchError(error => of(actions.startZookeeper.failure(error))),
    ),
  ).pipe(
    takeWhile(() => !skipList.includes(ACTIONS.START_ZOOKEEPER)),
    concatAll(),
  );

const startBroker$ = (params, skipList) =>
  of(
    defer(() => brokerApi.start(params)).pipe(
      delay(duration),
      map(() => actions.startBroker.request()),
      catchError(error => of(actions.startBroker.failure(error))),
    ),
  ).pipe(
    takeWhile(() => !skipList.includes(ACTIONS.START_BROKER)),
    concatAll(),
  );

const startWorker$ = (params, skipList) =>
  of(
    defer(() => workerApi.start(params)).pipe(
      delay(duration),
      map(() => actions.startWorker.request()),
      catchError(error => of(actions.startWorker.failure(error))),
    ),
  ).pipe(
    takeWhile(() => !skipList.includes(ACTIONS.START_WORKER)),
    concatAll(),
  );

const startTopic$ = topic =>
  of(
    defer(() => topicApi.start(topic)).pipe(
      delay(duration),
      map(() => actions.startTopic.request()),
      catchError(error => of(actions.startTopic.failure(error))),
    ),
  ).pipe(concatAll());

const startTopics$ = topics =>
  of(...topics).pipe(mergeMap(topics => startTopic$(topics)));

const waitStartZookeeper$ = params =>
  of(
    defer(() => zookeeperApi.get(params.zookeeperKey)).pipe(
      map(res => res.data),
      switchMap(data => {
        if (!data?.state || data.state !== SERVICE_STATE.RUNNING) {
          throw data;
        }
        return from([
          actions.updateWorkspace.trigger({
            zookeeper: data,
            ...params.workspaceKey,
          }),
          actions.startZookeeper.success(data),
        ]);
      }),
      retryWhen(error => error.pipe(delay(duration * 2), take(retry))),
      catchError(error => of(actions.startZookeeper.failure(error))),
    ),
  ).pipe(concatAll());

const waitStartBroker$ = params =>
  of(
    defer(() => brokerApi.get(params.brokerKey)).pipe(
      map(res => res.data),
      switchMap(data => {
        if (!data?.state || data.state !== SERVICE_STATE.RUNNING) {
          throw data;
        }
        return from([
          actions.updateWorkspace.trigger({
            broker: data,
            ...params.workspaceKey,
          }),
          actions.startBroker.success(data),
        ]);
      }),
      retryWhen(error => error.pipe(delay(duration * 2), take(retry))),
      catchError(error => of(actions.startBroker.failure(error))),
    ),
  ).pipe(concatAll());

const waitStartWorker$ = params =>
  of(
    defer(() => workerApi.get(params.workerKey)).pipe(
      map(res => res.data),
      switchMap(data => {
        if (!data?.state || data.state !== SERVICE_STATE.RUNNING) {
          throw data;
        }
        return from([
          actions.updateWorkspace.trigger({
            worker: data,
            ...params.workspaceKey,
          }),
          actions.startWorker.success(data),
        ]);
      }),
      retryWhen(error => error.pipe(delay(duration * 2), take(retry))),
      catchError(error => of(actions.startWorker.failure(error))),
    ),
  ).pipe(concatAll());

const waitStartTopic$ = params =>
  of(
    defer(() => topicApi.get(params)).pipe(
      map(res => res.data),
      map(data => {
        if (!data?.state || data.state !== SERVICE_STATE.RUNNING) {
          throw data;
        }
        return actions.startTopic.success(data);
      }),
      retryWhen(error => error.pipe(delay(duration * 2), take(retry))),
      catchError(error => of(actions.startTopic.failure(error))),
    ),
  ).pipe(concatAll());

const waitStartTopics$ = topics =>
  of(...topics).pipe(mergeMap(topics => waitStartTopic$(topics)));

const waitStopZookeeper$ = params =>
  of(
    defer(() => zookeeperApi.get(params)).pipe(
      map(res => res.data),
      map(data => {
        if (data?.state) {
          throw data;
        }
        workspaceApi.update({ ...params.workspaceKey, zookeeper: { ...data } });
        return actions.stopZookeeper.success(data);
      }),
      retryWhen(error => error.pipe(delay(duration * 2), take(retry))),
      catchError(error => of(actions.startZookeeper.failure(error))),
    ),
  ).pipe(concatAll());

const waitStopBroker$ = params =>
  of(
    defer(() => brokerApi.get(params)).pipe(
      map(res => res.data),
      map(data => {
        if (data?.state) {
          throw data;
        }
        workspaceApi.update({ ...params.workspaceKey, broker: { ...data } });
        return actions.stopBroker.success(data);
      }),
      retryWhen(error => error.pipe(delay(duration * 2), take(retry))),
      catchError(error => of(actions.startBroker.failure(error))),
    ),
  ).pipe(concatAll());

const waitStopWorker$ = params =>
  of(
    defer(() => workerApi.get(params)).pipe(
      map(res => res.data),
      map(data => {
        if (data?.state) {
          throw data;
        }
        workspaceApi.update({ ...params.workspaceKey, worker: { ...data } });
        return actions.stopWorker.success(data);
      }),
      retryWhen(error => error.pipe(delay(duration * 2), take(retry))),
      catchError(error => of(actions.startWorker.failure(error))),
    ),
  ).pipe(concatAll());

const waitStopTopic$ = params =>
  of(
    defer(() => topicApi.get(params)).pipe(
      map(res => res.data),
      map(data => {
        if (data?.state) {
          throw data;
        }
        return actions.stopTopic.success(data);
      }),
      retryWhen(error => error.pipe(delay(duration * 2), take(retry))),
      catchError(error => of(actions.stopTopic.failure(error))),
    ),
  ).pipe(concatAll());

const waitStopTopics$ = topics =>
  of(...topics).pipe(mergeMap(topics => waitStopTopic$(topics)));

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

export default (action$, state$) =>
  action$.pipe(
    ofType(actions.restartWorkspace.TRIGGER),
    withLatestFrom(state$),
    exhaustMap(([action]) => {
      const {
        workspace,
        zookeeper,
        broker,
        worker,
        skipList = [],
        isRollback = false,
        workerSettings = {},
        brokerSettings = {},
        zookeeperSettings = {},
        tmpWorker = {},
        tmpBroker = {},
        tmpZookeeper = {},
        topics = [],
      } = action.payload.values;

      const workspaceKey = workspace;
      const zookeeperKey = zookeeper;
      const brokerKey = broker;
      const workerKey = worker;

      return of(
        stopWorker$(workerKey, skipList),
        waitStopWorker$({ ...workerKey, workspaceKey }),
        updateWorker$(
          { objectKey: workerKey, tmp: tmpWorker, settings: workerSettings },
          isRollback,
        ),

        stopTopics$(topics),
        waitStopTopics$(topics),

        stopBroker$(brokerKey, skipList),
        waitStopBroker$({
          ...brokerKey,
          workspaceKey,
          settings: brokerSettings,
        }),
        updateBroker$({ objectKey: brokerKey, tmp: tmpBroker }, isRollback),

        stopZookeeper$(zookeeperKey, skipList),
        waitStopZookeeper$({
          ...zookeeperKey,
          workspaceKey,
          settings: zookeeperSettings,
        }),
        updateZookeeper$(
          { objectKey: zookeeperKey, tmp: tmpZookeeper },
          isRollback,
        ),

        startZookeeper$(zookeeperKey, skipList),
        waitStartZookeeper$({ zookeeperKey, workspaceKey }),

        startBroker$(brokerKey, skipList),
        waitStartBroker$({ brokerKey, workspaceKey }),

        startTopics$(topics),
        waitStartTopics$(topics),

        startWorker$(workerKey, skipList),
        waitStartWorker$({ workerKey, workspaceKey }),

        finalize$({ workerKey, workspaceKey }),
      ).pipe(
        concatAll(),
        catchError(error => of(actions.restartWorkspace.failure(error))),
        takeUntil(action$.pipe(ofType(actions.pauseRestartWorkspace.TRIGGER))),
      );
    }),
  );
