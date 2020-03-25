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
import {
  switchMap,
  map,
  startWith,
  catchError,
  concatAll,
  retryWhen,
  delay,
  take,
} from 'rxjs/operators';
import { ajax } from 'rxjs/ajax';

import * as actions from 'store/actions';
import * as schema from 'store/schema';
import * as nodeApi from 'api/nodeApi';

const createNodeEpic = action$ =>
  action$.pipe(
    ofType(actions.createNode.TRIGGER),
    map(action => action.payload),
    switchMap(values =>
      from(nodeApi.create(values)).pipe(
        map(res => normalize(res.data, schema.node)),
        map(entities => actions.createNode.success(entities)),
        startWith(actions.createNode.request()),
        catchError(res => of(actions.createNode.failure(res))),
      ),
    ),
  );

const updateNodeEpic = action$ =>
  action$.pipe(
    ofType(actions.updateNode.TRIGGER),
    map(action => action.payload),
    switchMap(values =>
      from(nodeApi.update(values)).pipe(
        map(res => normalize(res.data, schema.node)),
        map(entities => actions.updateNode.success(entities)),
        startWith(actions.updateNode.request()),
        catchError(res => of(actions.updateNode.failure(res))),
      ),
    ),
  );

const fetchNodesEpic = action$ =>
  action$.pipe(
    ofType(actions.fetchNodes.TRIGGER),
    map(action => action.payload),
    switchMap(() =>
      from(nodeApi.getAll()).pipe(
        map(res => normalize(res.data, [schema.node])),
        map(entities => actions.fetchNodes.success(entities)),
        startWith(actions.fetchNodes.request()),
        catchError(res => of(actions.fetchNodes.failure(res))),
      ),
    ),
  );

const checkNodes$ = values =>
  // wait #4438 change axios to observers
  ajax.get('/api/nodes').pipe(
    map(res => {
      if (res.response.find(node => node.hostname === values)) {
        throw res;
      }
      return res;
    }),
    retryWhen(error => error.pipe(delay(2000), take(5))),
    map(() => actions.deleteNode.success(values)),
    startWith(actions.fetchNodes.request()),
    catchError(res => of(actions.fetchNodes.failure(res))),
  );

const deleteNodesEpic = action$ =>
  action$.pipe(
    ofType(actions.deleteNode.TRIGGER),
    map(action => action.payload),
    switchMap(values =>
      of(
        from(nodeApi.remove({ hostname: values })).pipe(
          map(() => actions.deleteNode.request()),
          startWith(actions.deleteNode.request()),
          catchError(res => of(actions.deleteNode.failure(res))),
        ),
        checkNodes$(values),
      ).pipe(concatAll()),
    ),
  );
export default combineEpics(
  createNodeEpic,
  updateNodeEpic,
  fetchNodesEpic,
  deleteNodesEpic,
);
