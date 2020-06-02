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
import { defer, of, scheduled, asapScheduler, from } from 'rxjs';
import {
  catchError,
  concatAll,
  delay,
  exhaustMap,
  map,
  startWith,
  withLatestFrom,
} from 'rxjs/operators';
import { reset } from 'redux-form';

import { createZookeeper$ } from 'store/epics/zookeeper/createZookeeperEpic';
import { startZookeeper$ } from 'store/epics/zookeeper/startZookeeperEpic';
import { createBroker$ } from 'store/epics/broker/createBrokerEpic';
import { startBroker$ } from 'store/epics/broker/startBrokerEpic';
import { createWorker$ } from 'store/epics/worker/createWorkerEpic';
import { startWorker$ } from 'store/epics/worker/startWorkerEpic';
import * as workspaceApi from 'api/workspaceApi';
import { FORM } from 'const';
import { LOG_LEVEL } from 'const';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getKey } from 'utils/object';

const duration = 1000;

const createWorkspace$ = (values) =>
  defer(() => workspaceApi.create(values)).pipe(
    delay(duration),
    map((res) => res.data),
    map((data) =>
      actions.createWorkspace.success(normalize(data, schema.workspace)),
    ),
    startWith(actions.createWorkspace.request()),
  );

const finalize$ = (params) =>
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
    // Refetch node list after creation successfully in order to get the runtime data
    of(actions.fetchNodes.trigger()),
  ).pipe(concatAll());

export default (action$, state$) =>
  action$.pipe(
    ofType(actions.createWorkspace.TRIGGER),
    withLatestFrom(state$),
    exhaustMap(([action]) => {
      const { workspace, zookeeper, broker, worker } = action.payload;
      const workspaceKey = getKey(workspace);

      return scheduled(
        [
          createWorkspace$(workspace),
          createZookeeper$(zookeeper).pipe(delay(duration)),
          createBroker$(broker).pipe(delay(duration)),
          createWorker$(worker).pipe(delay(duration)),
          startZookeeper$(zookeeper).pipe(delay(duration)),
          startBroker$(broker).pipe(delay(duration)),
          startWorker$(worker).pipe(delay(duration)),
          finalize$(workspaceKey).pipe(delay(duration)),
        ],
        asapScheduler,
      ).pipe(
        concatAll(),
        catchError((err) =>
          from([
            actions.createWorkspace.failure(err),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]),
        ),
      );
    }),
  );
