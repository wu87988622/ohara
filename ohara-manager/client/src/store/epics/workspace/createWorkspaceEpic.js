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
import { from, of, timer } from 'rxjs';
import {
  catchError,
  delay,
  exhaustMap,
  filter,
  map,
  startWith,
  switchMap,
  take,
  withLatestFrom,
} from 'rxjs/operators';

import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import * as brokerApi from 'api/brokerApi';
import * as workerApi from 'api/workerApi';
import * as workspaceApi from 'api/workspaceApi';
import * as zookeeperApi from 'api/zookeeperApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';

const createWorkspace$ = action =>
  from(workspaceApi.create(action.payload?.workspaceSettings));

const createZookeeper$ = action =>
  from(zookeeperApi.create(action.payload?.zookeeperSettings));

const createBroker$ = action =>
  from(brokerApi.create(action.payload?.brokerSettings));

const createWorker$ = action =>
  from(workerApi.create(action.payload?.workerSettings));

const startZookeeper$ = action =>
  from(zookeeperApi.start(action.payload?.zookeeperSettings));

const waitZookeeper$ = action =>
  timer(0, 1000).pipe(
    switchMap(() =>
      from(zookeeperApi.get(action.payload?.zookeeperSettings)).pipe(
        map(res => res.data),
        filter(data => data.state === SERVICE_STATE.RUNNING),
      ),
    ),
    take(1),
  );

const startBroker$ = action =>
  from(brokerApi.start(action.payload?.brokerSettings));

const waitBroker$ = action =>
  timer(0, 1000).pipe(
    switchMap(() =>
      from(brokerApi.get(action.payload?.brokerSettings)).pipe(
        map(res => res.data),
        filter(data => data.state === SERVICE_STATE.RUNNING),
      ),
    ),
    take(1),
  );

const startWorker$ = action =>
  from(workerApi.start(action.payload?.workerSettings));

const waitWorker$ = action =>
  timer(0, 1000).pipe(
    switchMap(() =>
      from(workerApi.get(action.payload?.workerSettings)).pipe(
        map(res => res.data),
        filter(data => data.state === SERVICE_STATE.RUNNING),
      ),
    ),
    take(1),
  );

export default (action$, state$) =>
  action$.pipe(
    ofType(actions.createWorkspace.TRIGGER),
    withLatestFrom(state$),
    exhaustMap(([action]) =>
      createWorkspace$(action).pipe(
        delay(1000),
        switchMap(workspaceRes =>
          createZookeeper$(action).pipe(
            delay(1000),
            switchMap(() =>
              createBroker$(action).pipe(
                delay(1000),
                switchMap(() =>
                  createWorker$(action).pipe(
                    delay(1000),
                    switchMap(() =>
                      startZookeeper$(action).pipe(
                        delay(3000),
                        switchMap(() =>
                          waitZookeeper$(action).pipe(
                            switchMap(() =>
                              startBroker$(action).pipe(
                                delay(3000),
                                switchMap(() =>
                                  waitBroker$(action).pipe(
                                    switchMap(() =>
                                      startWorker$(action).pipe(
                                        delay(3000),
                                        switchMap(() =>
                                          waitWorker$(action).pipe(
                                            switchMap(() =>
                                              of(
                                                actions.createWorkspace.success(
                                                  normalize(
                                                    workspaceRes.data,
                                                    schema.workspace,
                                                  ),
                                                ),
                                                actions.switchWorkspace.trigger(
                                                  workspaceRes.data.name,
                                                ),
                                              ),
                                            ),
                                            startWith(
                                              actions.startWorker.success(),
                                            ),
                                          ),
                                        ),
                                        startWith(
                                          actions.startWorker.request(),
                                        ),
                                        startWith(
                                          actions.startBroker.success(),
                                        ),
                                      ),
                                    ),
                                  ),
                                ),
                                startWith(actions.startBroker.request()),
                                startWith(actions.startZookeeper.success()),
                              ),
                            ),
                          ),
                        ),
                        startWith(actions.startZookeeper.request()),
                        startWith(actions.createWorker.success()),
                      ),
                    ),
                    startWith(actions.createWorker.request()),
                    startWith(actions.createBroker.success()),
                  ),
                ),
                startWith(actions.createBroker.request()),
                startWith(actions.createZookeeper.success()),
              ),
            ),
            startWith(actions.createZookeeper.request()),
          ),
        ),
        startWith(actions.createWorkspace.request()),
        catchError(error => of(actions.createWorkspace.failure(error))),
      ),
    ),
  );
