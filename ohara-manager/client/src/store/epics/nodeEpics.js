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
import { of, defer, iif, throwError, zip, from } from 'rxjs';
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

import { LOG_LEVEL } from 'const';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import * as nodeApi from 'api/nodeApi';

export const createNodeEpic = action$ =>
  action$.pipe(
    ofType(actions.createNode.TRIGGER),
    map(action => action.payload),
    distinctUntilChanged(),
    mergeMap(({ params, options }) =>
      defer(() => nodeApi.create(params)).pipe(
        map(res => normalize(res.data, schema.node)),
        map(entities => {
          if (options?.onSuccess) {
            options.onSuccess();
          }
          return actions.createNode.success(entities);
        }),
        startWith(actions.createNode.request()),
        catchError(err => {
          if (options?.onError) {
            options.onError(err);
          }
          return from([
            actions.createNode.failure(err),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]);
        }),
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
        catchError(err =>
          from([
            actions.updateNode.failure(err),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]),
        ),
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
        catchError(err =>
          from([
            actions.fetchNodes.failure(err),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]),
        ),
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
              throwError({
                data: value?.data,
                meta: value?.meta,
                title: `Try to remove node: "${hostname}" failed after retry ${index} times.`,
              }),
              of(value).pipe(delay(2000)),
            ),
          ),
        ),
      ),
    ),
  ).pipe(
    map(() => actions.deleteNode.success(hostname)),
    startWith(actions.deleteNode.request()),
    catchError(err =>
      from([
        actions.deleteNode.failure(err),
        actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
      ]),
    ),
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
