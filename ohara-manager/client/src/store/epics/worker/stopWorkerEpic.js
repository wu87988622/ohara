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
import { merge } from 'lodash';
import { ofType } from 'redux-observable';
import { defer, of, iif, throwError, zip, from } from 'rxjs';
import {
  catchError,
  delay,
  map,
  retryWhen,
  startWith,
  concatMap,
  distinctUntilChanged,
  mergeMap,
} from 'rxjs/operators';

import { LOG_LEVEL } from 'const';
import * as workerApi from 'api/workerApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';

const stopWorker$ = params => {
  const workerId = getId(params);
  return zip(
    defer(() => workerApi.stop(params)),
    defer(() => workerApi.get(params)).pipe(
      map(res => {
        if (res.data?.state) throw res;
        else return res.data;
      }),

      retryWhen(errors =>
        errors.pipe(
          concatMap((value, index) =>
            iif(
              () => index > 10,
              throwError({ title: 'stop worker exceeded max retry count' }),
              of(value).pipe(delay(2000)),
            ),
          ),
        ),
      ),
    ),
  ).pipe(
    map(([, data]) => normalize(data, schema.worker)),
    map(normalizedData => merge(normalizedData, { workerId })),
    map(normalizedData => actions.stopWorker.success(normalizedData)),
    startWith(actions.stopWorker.request({ workerId })),
    catchError(err =>
      from([
        actions.stopWorker.failure(merge(err, { workerId })),
        actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
      ]),
    ),
  );
};

export default action$ =>
  action$.pipe(
    ofType(actions.stopWorker.TRIGGER),
    map(action => action.payload),
    distinctUntilChanged(),
    mergeMap(params => stopWorker$(params)),
  );
