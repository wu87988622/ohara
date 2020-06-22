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

import { throwError, zip } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { retryBackoff } from 'backoff-rxjs';
import { ObjectKey } from 'api/apiInterface/basicInterface';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import * as workerApi from 'api/workerApi';
import { deferApi } from './internal/deferApi';
import { isServiceRunning } from './utils';

export function createWorker(values: any) {
  return deferApi(() => workerApi.create(values)).pipe(map((res) => res.data));
}

export function fetchWorker(values: ObjectKey) {
  return deferApi(() => workerApi.get(values)).pipe(map((res) => res.data));
}

export function startWorker(key: ObjectKey) {
  return zip(
    // attempt to start at intervals
    deferApi(() => workerApi.start(key)),
    // wait until the service is running
    deferApi(() => workerApi.get(key)).pipe(
      map((res) => {
        if (!isServiceRunning(res)) throw res;
        return res.data;
      }),
    ),
  ).pipe(
    map(([, data]) => data),
    // retry every 2 seconds, up to 10 times
    retryBackoff({
      initialInterval: 2000,
      maxRetries: 10,
      maxInterval: 2000,
    }),
    catchError((error) =>
      throwError({
        data: error?.data,
        meta: error?.meta,
        title:
          `Try to start worker: "${key.name}" failed after retry 10 times. ` +
          `Expected state: ${SERVICE_STATE.RUNNING}, Actual state: ${error.data.state}`,
      }),
    ),
  );
}

export function stopWorker(key: ObjectKey) {
  return zip(
    // attempt to stop at intervals
    deferApi(() => workerApi.stop(key)),
    // wait until the service is not running
    deferApi(() => workerApi.get(key)).pipe(
      map((res) => {
        if (isServiceRunning(res)) throw res;
        return res.data;
      }),
    ),
  ).pipe(
    map(([, data]) => data),
    // retry every 2 seconds, up to 10 times
    retryBackoff({
      initialInterval: 2000,
      maxRetries: 10,
      maxInterval: 2000,
    }),
    catchError((error) =>
      throwError({
        data: error?.data,
        meta: error?.meta,
        title:
          `Try to stop worker: "${key.name}" failed after retry 10 times. ` +
          `Expected state is nonexistent, Actual state: ${error.data.state}`,
      }),
    ),
  );
}

export function deleteWorker(key: ObjectKey) {
  return deferApi(() => workerApi.remove(key));
}
