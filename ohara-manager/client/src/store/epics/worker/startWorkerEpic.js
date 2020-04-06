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
import { defer, interval, of } from 'rxjs';
import {
  catchError,
  concatAll,
  debounce,
  delay,
  map,
  retryWhen,
  startWith,
  switchMap,
  take,
} from 'rxjs/operators';

import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import * as workerApi from 'api/workerApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';

export const startWorker$ = params => {
  const workerId = getId(params);
  return of(
    defer(() => workerApi.start(params)),
    defer(() => workerApi.get(params)).pipe(
      map(res => {
        if (!res.data?.state) throw res;
        if (res.data.state !== SERVICE_STATE.RUNNING) throw res;
        return res.data;
      }),
      map(data => normalize(data, schema.worker)),
      map(normalizedData => merge(normalizedData, { workerId })),
      map(normalizedData => actions.startWorker.success(normalizedData)),
      retryWhen(error => error.pipe(delay(2000), take(10))),
    ),
  ).pipe(
    concatAll(),
    startWith(actions.startWorker.request({ workerId })),
    catchError(error =>
      of(actions.startWorker.failure(merge(error, { workerId }))),
    ),
  );
};

export default action$ =>
  action$.pipe(
    ofType(actions.startWorker.TRIGGER),
    map(action => action.payload),
    debounce(() => interval(1000)),
    switchMap(params => startWorker$(params)),
  );
