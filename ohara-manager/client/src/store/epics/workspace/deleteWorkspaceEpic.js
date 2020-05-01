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

const deleteWorkspace$ = (values, isRollback) =>
  of(
    defer(() => workspaceApi.remove(values)).pipe(
      delay(duration),
      map(res => res.data),
      map(() => actions.deleteWorkspace.success(getId(values))),
      startWith(actions.deleteWorkspace.request()),
    ),
  ).pipe(
    takeWhile(() => !isRollback),
    concatAll(),
  );

const deleteZookeeper$ = (values, isRollback, skipList) =>
  of(
    defer(() => zookeeperApi.remove(values)).pipe(
      delay(duration),
      map(res => res.data),
      map(data => actions.deleteZookeeper.success(data)),
      startWith(actions.deleteZookeeper.request()),
    ),
  ).pipe(
    takeWhile(
      () => !isRollback || !skipList.includes(ACTIONS.DELETE_ZOOKEEPER),
    ),
    concatAll(),
  );

const deleteBroker$ = (values, isRollback, skipList) =>
  of(
    defer(() => brokerApi.remove(values)).pipe(
      delay(duration),
      map(res => res.data),
      map(data => actions.deleteBroker.success(data)),
      startWith(actions.deleteBroker.request()),
    ),
  ).pipe(
    takeWhile(() => !isRollback || !skipList.includes(ACTIONS.DELETE_BROKER)),
    concatAll(),
  );

const deleteWorker$ = (values, isRollback, skipList) =>
  of(
    defer(() => workerApi.remove(values)).pipe(
      delay(duration),
      map(res => res.data),
      map(data => actions.deleteWorker.success(data)),
      startWith(actions.deleteWorker.request()),
    ),
  ).pipe(
    takeWhile(() => !isRollback || !skipList.includes(ACTIONS.DELETE_WORKER)),
    concatAll(),
  );

const stopZookeeper$ = (params, skipList) =>
  of(
    defer(() => zookeeperApi.stop(params)).pipe(
      delay(duration),
      map(() => actions.stopZookeeper.request()),
      catchError(error => of(actions.stopZookeeper.failure(error))),
    ),
  ).pipe(
    takeWhile(() => !skipList.includes(ACTIONS.STOP_ZOOKEEPER)),
    concatAll(),
  );

const stopBroker$ = (params, skipList) =>
  of(
    defer(() => brokerApi.stop(params)).pipe(
      delay(duration),
      map(() => actions.stopBroker.request()),
      catchError(error => of(actions.stopBroker.failure(error))),
    ),
  ).pipe(
    takeWhile(() => !skipList.includes(ACTIONS.STOP_BROKER)),
    concatAll(),
  );

const stopWorker$ = (params, skipList) =>
  of(
    defer(() => workerApi.stop(params)).pipe(
      delay(duration),
      map(() => actions.stopWorker.request()),
      catchError(error => of(actions.stopWorker.failure(error))),
    ),
  ).pipe(
    takeWhile(() => !skipList.includes(ACTIONS.STOP_WORKER)),
    concatAll(),
  );

const createZookeeper$ = (values, skipList) =>
  of(
    defer(() => zookeeperApi.create(values)).pipe(
      delay(duration),
      map(res => res.data),
      map(data => actions.createZookeeper.success(data)),
      startWith(actions.createZookeeper.request()),
    ),
  ).pipe(
    takeWhile(() => skipList.includes(ACTIONS.DELETE_ZOOKEEPER)),
    concatAll(),
  );

const createBroker$ = (values, skipList) =>
  of(
    defer(() => brokerApi.create(values)).pipe(
      delay(duration),
      map(res => res.data),
      map(data => actions.createBroker.success(data)),
      startWith(actions.createBroker.request()),
    ),
  ).pipe(
    takeWhile(() => skipList.includes(ACTIONS.DELETE_BROKER)),
    concatAll(),
  );

const createWorker$ = (values, skipList) =>
  of(
    defer(() => workerApi.create(values)).pipe(
      delay(duration),
      map(res => res.data),
      map(data => actions.createWorker.success(data)),
      startWith(actions.createWorker.request()),
    ),
  ).pipe(
    takeWhile(() => skipList.includes(ACTIONS.DELETE_WORKER)),
    concatAll(),
  );

const startZookeeper$ = (params, skipList) =>
  of(
    defer(() => zookeeperApi.start(params)).pipe(
      delay(duration),
      map(() => actions.startZookeeper.request()),
      catchError(error => of(actions.startZookeeper.failure(error))),
    ),
  ).pipe(
    takeWhile(() => skipList.includes(ACTIONS.STOP_ZOOKEEPER)),
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
    takeWhile(() => skipList.includes(ACTIONS.STOP_BROKER)),
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
    takeWhile(() => skipList.includes(ACTIONS.STOP_WORKER)),
    concatAll(),
  );

const waitStartZookeeper$ = (params, skipList) =>
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
  ).pipe(
    takeWhile(() => skipList.includes(ACTIONS.STOP_ZOOKEEPER)),
    concatAll(),
  );

const waitStartBroker$ = (params, skipList) =>
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
  ).pipe(
    takeWhile(() => skipList.includes(ACTIONS.STOP_BROKER)),
    concatAll(),
  );

const waitStartWorker$ = (params, skipList) =>
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
  ).pipe(
    takeWhile(() => skipList.includes(ACTIONS.STOP_WORKER)),
    concatAll(),
  );

const waitStopZookeeper$ = (params, skipList) =>
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
  ).pipe(
    takeWhile(() => !skipList.includes(ACTIONS.START_ZOOKEEPER)),
    concatAll(),
  );

const waitStopBroker$ = (params, skipList) =>
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
  ).pipe(
    takeWhile(() => !skipList.includes(ACTIONS.START_BROKER)),
    concatAll(),
  );

const waitStopWorker$ = (params, skipList) =>
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
  ).pipe(
    takeWhile(() => !skipList.includes(ACTIONS.START_WORKER)),
    concatAll(),
  );

const finalize$ = params =>
  of(
    of(actions.switchCreateWorkspaceStep(0)),
    of(
      actions.createEventLog.trigger({
        title: `Successfully Delete workspace ${params.name}.`,
        type: LOG_LEVEL.info,
      }),
    ),
  ).pipe(concatAll());

export default (action$, state$) =>
  action$.pipe(
    ofType(actions.deleteWorkspace.TRIGGER),
    withLatestFrom(state$),
    exhaustMap(([action]) => {
      const {
        workspace,
        zookeeper,
        broker,
        worker,
        skipList = [],
        isRollback = false,
        tmpWorker = {},
        tmpBroker = {},
        tmpZookeeper = {},
      } = action.payload;

      const workspaceKey = workspace;
      const zookeeperKey = zookeeper;
      const brokerKey = broker;
      const workerKey = worker;
      return of(
        createZookeeper$(tmpZookeeper, skipList),
        startZookeeper$(zookeeperKey, skipList),
        waitStartZookeeper$(zookeeperKey, skipList),

        createBroker$(tmpBroker, skipList),
        startBroker$(brokerKey, skipList),
        waitStartBroker$(brokerKey, skipList),

        createWorker$(tmpWorker, skipList),
        startWorker$(workerKey, skipList),
        waitStartWorker$(workerKey, skipList),

        stopWorker$(workerKey, skipList),
        waitStopWorker$({ ...workerKey, workspaceKey }, skipList),

        stopBroker$(brokerKey, skipList),
        waitStopBroker$({ ...brokerKey, workspaceKey }, skipList),

        stopZookeeper$(zookeeperKey, skipList),
        waitStopZookeeper$({ ...zookeeperKey, workspaceKey }, skipList),

        deleteWorker$(workerKey, isRollback, skipList),
        deleteBroker$(brokerKey, isRollback, skipList),
        deleteZookeeper$(zookeeperKey, isRollback, skipList),
        deleteWorkspace$(workspaceKey, isRollback),

        finalize$(workspaceKey),
      ).pipe(
        concatAll(),
        catchError(err =>
          from([
            actions.deleteWorkspace.failure(err),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]),
        ),
        takeUntil(action$.pipe(ofType(actions.pauseDeleteWorkspace.TRIGGER))),
      );
    }),
  );
