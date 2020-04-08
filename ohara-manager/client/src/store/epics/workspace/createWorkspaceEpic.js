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
} from 'rxjs/operators';
import { reset } from 'redux-form';

import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import * as brokerApi from 'api/brokerApi';
import * as workerApi from 'api/workerApi';
import * as workspaceApi from 'api/workspaceApi';
import * as zookeeperApi from 'api/zookeeperApi';
import { FORM } from 'const';
import { LOG_LEVEL } from 'const';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getKey } from 'utils/object';

const duration = 1000;
const retry = 10;

const createWorkspace$ = values =>
  defer(() => workspaceApi.create(values)).pipe(
    delay(duration),
    map(res => res.data),
    map(data =>
      actions.createWorkspace.success(normalize(data, schema.workspace)),
    ),
    startWith(actions.createWorkspace.request()),
  );

const createZookeeper$ = values =>
  defer(() => zookeeperApi.create(values)).pipe(
    delay(duration),
    map(res => res.data),
    map(data => actions.createZookeeper.success(data)),
    startWith(actions.createZookeeper.request()),
  );

const createBroker$ = values =>
  defer(() => brokerApi.create(values)).pipe(
    delay(duration),
    map(res => res.data),
    map(data => actions.createBroker.success(data)),
    startWith(actions.createBroker.request()),
  );

const createWorker$ = values =>
  defer(() => workerApi.create(values)).pipe(
    delay(duration),
    map(res => res.data),
    map(data => actions.createWorker.success(data)),
    startWith(actions.createWorker.request()),
  );

const startZookeeper$ = params =>
  defer(() => zookeeperApi.start(params)).pipe(
    delay(duration),
    map(() => actions.startZookeeper.request()),
    catchError(error => of(actions.startZookeeper.failure(error))),
  );

const startBroker$ = params =>
  defer(() => brokerApi.start(params)).pipe(
    delay(duration),
    map(() => actions.startBroker.request()),
    catchError(error => of(actions.startBroker.failure(error))),
  );

const startWorker$ = params =>
  defer(() => workerApi.start(params)).pipe(
    delay(duration),
    map(() => actions.startWorker.request()),
    catchError(error => of(actions.startWorker.failure(error))),
  );

const waitZookeeper$ = params =>
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
  );

const waitBroker$ = params =>
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
  );

const waitWorker$ = params =>
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
  );

const finalize$ = params =>
  of(
    of(actions.createWorkspace.fulfill()),
    // Clear form data
    of(reset(FORM.CREATE_WORKSPACE)),
    // Back to the first step
    of(actions.switchCreateWorkspaceStep(0)),
    // Close all dialogs
    of(actions.closeCreateWorkspace.trigger()),
    of(actions.closeIntro.trigger()),
    // Log a success message to Event Log
    of(
      actions.createEventLog.trigger({
        title: `Successfully created workspace ${params.name}.`,
        type: LOG_LEVEL.info,
      }),
    ),
    // Switch to the workspace you just created
    of(actions.switchWorkspace.trigger(params)),
  ).pipe(concatAll());

export default (action$, state$) =>
  action$.pipe(
    ofType(actions.createWorkspace.TRIGGER),
    withLatestFrom(state$),
    exhaustMap(([action]) => {
      const { workspace, zookeeper, broker, worker } = action.payload;
      const workspaceKey = getKey(workspace);
      const zookeeperKey = getKey(zookeeper);
      const brokerKey = getKey(broker);
      const workerKey = getKey(worker);
      return of(
        // TODO: upload files
        createWorkspace$(workspace),
        createZookeeper$(zookeeper),
        createBroker$(broker),
        createWorker$(worker),
        startZookeeper$(zookeeperKey),
        waitZookeeper$(zookeeperKey),
        startBroker$(brokerKey),
        waitBroker$(brokerKey),
        startWorker$(workerKey),
        waitWorker$(workerKey),
        finalize$(workspaceKey),
      ).pipe(
        concatAll(),
        catchError(error => of(actions.createWorkspace.failure(error))),
      );
    }),
  );
