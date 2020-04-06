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
import { combineEpics, ofType } from 'redux-observable';
import { from, of } from 'rxjs';
import { switchMap, map, startWith, catchError } from 'rxjs/operators';

import * as workerApi from 'api/workerApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';

const createWorkerEpic = action$ =>
  action$.pipe(
    ofType(actions.createWorker.TRIGGER),
    map(action => action.payload),
    switchMap(values =>
      from(workerApi.create(values)).pipe(
        map(res => normalize(res.data, schema.worker)),
        map(entities => actions.createWorker.success(entities)),
        startWith(actions.createWorker.request()),
        catchError(res => of(actions.createWorker.failure(res))),
      ),
    ),
  );

const updateWorkerEpic = action$ =>
  action$.pipe(
    ofType(actions.updateWorker.TRIGGER),
    map(action => action.payload),
    switchMap(values =>
      from(workerApi.update(values)).pipe(
        map(res => normalize(res.data, schema.worker)),
        map(entities => actions.updateWorker.success(entities)),
        startWith(actions.updateWorker.request()),
        catchError(res => of(actions.updateWorker.failure(res))),
      ),
    ),
  );

const startWorkerEpic = action$ =>
  action$.pipe(
    ofType(actions.startWorker.TRIGGER),
    map(action => action.payload),
    switchMap(params =>
      from(workerApi.start(params)).pipe(
        map(res => normalize(res.data, schema.worker)),
        map(entities => actions.startWorker.success(entities)),
        startWith(actions.startWorker.request()),
        catchError(res => of(actions.startWorker.failure(res))),
      ),
    ),
  );

const stopWorkerEpic = action$ =>
  action$.pipe(
    ofType(actions.stopWorker.TRIGGER),
    map(action => action.payload),
    switchMap(params =>
      from(workerApi.stop(params)).pipe(
        map(res => normalize(res.data, schema.worker)),
        map(entities => actions.stopWorker.success(entities)),
        startWith(actions.stopWorker.request()),
        catchError(res => of(actions.stopWorker.failure(res))),
      ),
    ),
  );

const deleteWorkerEpic = action$ =>
  action$.pipe(
    ofType(actions.deleteWorker.TRIGGER),
    map(action => action.payload),
    switchMap(params =>
      from(workerApi.remove(params)).pipe(
        map(params => getId(params)),
        map(id => actions.deletePipeline.success(id)),
        startWith(actions.deleteWorker.request()),
        catchError(res => of(actions.deleteWorker.failure(res))),
      ),
    ),
  );

export default combineEpics(
  createWorkerEpic,
  updateWorkerEpic,
  startWorkerEpic,
  stopWorkerEpic,
  deleteWorkerEpic,
);
