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

import { merge } from 'lodash';
import { ofType } from 'redux-observable';
import { defer, from, zip, iif, of, throwError } from 'rxjs';
import {
  catchError,
  map,
  startWith,
  mergeMap,
  distinctUntilChanged,
  retryWhen,
  concatMap,
  delay,
  takeUntil,
} from 'rxjs/operators';

import { LOG_LEVEL } from 'const';
import * as workerApi from 'api/workerApi';
import { deleteWorker } from 'observables';
import * as actions from 'store/actions';
import { getId, getKey } from 'utils/object';

// Note: The caller SHOULD handle the error of this action
export const deleteWorker$ = (params) => {
  const workerId = getId(params);
  return zip(
    defer(() => workerApi.remove(params)),
    defer(() => workerApi.getAll({ group: params.group })).pipe(
      map((res) => {
        if (
          res.data.length > 0 &&
          res.data.map((value) => value.name).includes(params.name)
        )
          throw res;
        else return res.data;
      }),
    ),
  ).pipe(
    retryWhen((errors) =>
      errors.pipe(
        concatMap((value, index) =>
          iif(
            () => index > 10,
            throwError({
              data: value?.data,
              meta: value?.meta,
              title: 'delete worker exceeded max retry count',
            }),
            of(value).pipe(delay(2000)),
          ),
        ),
      ),
    ),
    map(() => actions.deleteWorker.success({ workerId })),
    startWith(actions.deleteWorker.request({ workerId })),
  );
};

export default (action$) =>
  action$.pipe(
    ofType(actions.deleteWorker.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap(({ values, resolve, reject }) => {
      const workerId = getId(values);
      const workerKey = getKey(values);

      return deleteWorker(workerKey).pipe(
        map(() => {
          if (resolve) resolve();
          return actions.deleteWorker.success({ workerId });
        }),
        startWith(actions.deleteWorker.request({ workerId })),
        catchError((err) => {
          if (reject) reject(err);
          return from([
            actions.deleteWorker.failure(merge(err, { workerId })),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]);
        }),
        takeUntil(action$.pipe(ofType(actions.deleteWorker.CANCEL))),
      );
    }),
  );
