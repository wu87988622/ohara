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
import { of, defer, iif, throwError, zip } from 'rxjs';
import {
  switchMap,
  map,
  startWith,
  catchError,
  retryWhen,
  delay,
  concatMap,
  distinctUntilChanged,
  mergeMap,
  throttleTime,
} from 'rxjs/operators';

import * as actions from 'store/actions';
import * as schema from 'store/schema';
import * as nodeApi from 'api/nodeApi';

export const createNodeEpic = action$ =>
  action$.pipe(
    ofType(actions.createNode.TRIGGER),
    map(action => action.payload),
    distinctUntilChanged(),
    mergeMap(values =>
      defer(() => nodeApi.create(values)).pipe(
        map(res => normalize(res.data, schema.node)),
        map(entities => actions.createNode.success(entities)),
        startWith(actions.createNode.request()),
        catchError(res => of(actions.createNode.failure(res))),
      ),
    ),
  );

export const updateNodeEpic = action$ =>
  action$.pipe(
    ofType(actions.updateNode.TRIGGER),
    map(action => action.payload),
    mergeMap(values =>
      defer(() => nodeApi.update(values)).pipe(
        map(res => normalize(res.data, schema.node)),
        map(entities => actions.updateNode.success(entities)),
        startWith(actions.updateNode.request()),
        catchError(res => of(actions.updateNode.failure(res))),
      ),
    ),
  );

export const fetchNodesEpic = action$ =>
  action$.pipe(
    ofType(actions.fetchNodes.TRIGGER),
    throttleTime(1000),
    switchMap(() =>
      defer(() => nodeApi.getAll()).pipe(
        map(res => normalize(res.data, [schema.node])),
        map(entities => actions.fetchNodes.success(entities)),
        startWith(actions.fetchNodes.request()),
        catchError(res => of(actions.fetchNodes.failure(res))),
      ),
    ),
  );

const deleteNode$ = hostname =>
  zip(
    defer(() => nodeApi.remove(hostname)),
    defer(() => nodeApi.getAll()).pipe(
      map(res => {
        if (res.data.find(node => node.hostname === hostname)) throw res;
        else return res.data;
      }),
      retryWhen(errors =>
        errors.pipe(
          concatMap((value, index) =>
            iif(
              () => index > 4,
              throwError('exceed max retry times'),
              of(value).pipe(delay(2000)),
            ),
          ),
        ),
      ),
    ),
  ).pipe(
    map(() => actions.deleteNode.success(hostname)),
    startWith(actions.deleteNode.request()),
    catchError(error => of(actions.deleteNode.failure(error))),
  );

export const deleteNodeEpic = action$ =>
  action$.pipe(
    ofType(actions.deleteNode.TRIGGER),
    map(action => action.payload),
    distinctUntilChanged(),
    mergeMap(hostname => deleteNode$(hostname)),
  );

export default combineEpics(
  createNodeEpic,
  updateNodeEpic,
  fetchNodesEpic,
  deleteNodeEpic,
);
