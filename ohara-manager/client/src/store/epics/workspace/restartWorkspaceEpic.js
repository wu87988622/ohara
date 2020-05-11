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

import { ofType } from 'redux-observable';
import { defer, of } from 'rxjs';
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
} from 'rxjs/operators';

import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import * as brokerApi from 'api/brokerApi';
import * as workerApi from 'api/workerApi';
import * as workspaceApi from 'api/workspaceApi';
import * as zookeeperApi from 'api/zookeeperApi';
import { ACTIONS } from 'const';
import { LOG_LEVEL } from 'const';
import * as actions from 'store/actions';
import { getId } from 'utils/object';

const duration = 1000;
const retry = 10;

const updateWorkspace$ = (values, isRollback) =>
  of(
    defer(() =>
      workspaceApi.update({
        name: values.name,
        group: values.group,
        worker: null,
        broker: null,
        zookeeper: null,
      }),
    ).pipe(
      delay(duration),
      map(res => res.data),
      map(() => actions.updateWorkspace.success(getId(values))),
      startWith(actions.updateWorkspace.request()),
    ),
  ).pipe(
    takeWhile(() => !isRollback),
    concatAll(),
  );

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
      map(data => actions.updateZookeeper.success(data)),
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
      map(data => actions.updateBroker.success(data)),
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
      map(data => actions.updateWorker.success(data)),
      startWith(actions.updateWorker.request()),
    ),
  ).pipe(
    takeWhile(() => !isRollback),
    concatAll(),
  );

const updateOldZookeeper$ = (values, isRollback) =>
  of(
    defer(() =>
      zookeeperApi.update({
        ...values.values.tmp,
        name: values.objectKey.name,
        group: values.objectKey.group,
      }),
    ).pipe(
      delay(duration),
      map(res => res.data),
      map(data => actions.updateZookeeper.success(data)),
      startWith(actions.updateZookeeper.request()),
    ),
  ).pipe(
    takeWhile(() => isRollback),
    concatAll(),
  );

const updateOldBroker$ = (values, isRollback) =>
  of(
    defer(() =>
      brokerApi.update({
        ...values.values.tmp,
        name: values.objectKey.name,
        group: values.objectKey.group,
      }),
    ).pipe(
      delay(duration),
      map(res => res.data),
      map(data => actions.updateBroker.success(data)),
      startWith(actions.updateBroker.request()),
    ),
  ).pipe(
    takeWhile(() => isRollback),
    concatAll(),
  );

const updateOldWorker$ = (values, isRollback) =>
  of(
    defer(() =>
      workerApi.update({
        ...values.values.tmp,
        name: values.objectKey.name,
        group: values.objectKey.group,
      }),
    ).pipe(
      delay(duration),
      map(res => res.data),
      map(data => actions.updateWorker.success(data)),
      startWith(actions.updateWorker.request()),
    ),
  ).pipe(
    takeWhile(() => isRollback),
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

const waitStartZookeeper$ = params =>
  of(
    defer(() => zookeeperApi.get(params)).pipe(
      map(res => res.data),
      map(data => {
        if (!data?.state || data.state !== SERVICE_STATE.RUNNING) {
          throw data;
        }
        return actions.startZookeeper.success(data);
      }),
      retryWhen(error => error.pipe(delay(duration * 2), take(retry))),
      catchError(error => of(actions.startZookeeper.failure(error))),
    ),
  ).pipe(concatAll());

const waitStartBroker$ = params =>
  of(
    defer(() => brokerApi.get(params)).pipe(
      map(res => res.data),
      map(data => {
        if (!data?.state || data.state !== SERVICE_STATE.RUNNING) {
          throw data;
        }
        return actions.startBroker.success(data);
      }),
      retryWhen(error => error.pipe(delay(duration * 2), take(retry))),
      catchError(error => of(actions.startBroker.failure(error))),
    ),
  ).pipe(concatAll());

const waitStartWorker$ = params =>
  of(
    defer(() => workerApi.get(params)).pipe(
      map(res => res.data),
      map(data => {
        if (!data?.state || data.state !== SERVICE_STATE.RUNNING) {
          throw data;
        }
        return actions.startWorker.success(data);
      }),
      retryWhen(error => error.pipe(delay(duration * 2), take(retry))),
      catchError(error => of(actions.startWorker.failure(error))),
    ),
  ).pipe(concatAll());

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

const finalize$ = params =>
  of(
    of(
      actions.createEventLog.trigger({
        title: `Successfully Restart workspace ${params.name}.`,
        type: LOG_LEVEL.info,
      }),
    ),
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
      } = action.payload;

      const workspaceKey = workspace;
      const zookeeperKey = zookeeper;
      const brokerKey = broker;
      const workerKey = worker;
      return of(
        stopWorker$(workerKey, skipList),
        waitStopWorker$({ ...workerKey, workspaceKey }),
        updateOldWorker$({ objectKey: workerKey, tmp: tmpWorker }, isRollback),
        updateWorker$(
          { objectKey: workerKey, tmp: tmpWorker, settings: workerSettings },
          isRollback,
        ),

        stopBroker$(brokerKey, skipList),
        waitStopBroker$({
          ...brokerKey,
          workspaceKey,
          settings: brokerSettings,
        }),
        updateOldBroker$(
          { objectKey: brokerKey, tmp: tmpZookeeper },
          isRollback,
        ),
        updateBroker$({ objectKey: brokerKey, tmp: tmpBroker }, isRollback),

        stopZookeeper$(zookeeperKey, skipList),
        waitStopZookeeper$({
          ...zookeeperKey,
          workspaceKey,
          settings: zookeeperSettings,
        }),
        updateOldZookeeper$(
          { objectKey: zookeeperKey, tmp: tmpZookeeper },
          isRollback,
        ),
        updateZookeeper$(
          { objectKey: zookeeperKey, tmp: tmpZookeeper },
          isRollback,
        ),

        startZookeeper$(zookeeperKey, skipList),
        waitStartZookeeper$(zookeeperKey),

        startBroker$(brokerKey, skipList),
        waitStartBroker$(brokerKey),

        startWorker$(workerKey, skipList),
        waitStartWorker$(workerKey),

        updateWorkspace$(workspaceKey, isRollback),

        finalize$(workspaceKey),
      ).pipe(
        concatAll(),
        catchError(error => of(actions.restartWorkspace.failure(error))),
        takeUntil(action$.pipe(ofType(actions.pauseRestartWorkspace.TRIGGER))),
      );
    }),
  );
