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

import { defer, of, throwError } from 'rxjs';
import { catchError, map, concatAll, last } from 'rxjs/operators';
import { retryBackoff } from 'backoff-rxjs';
import { ObjectKey } from 'api/apiInterface/basicInterface';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import * as workerApi from 'api/workerApi';
import { RETRY_CONFIG } from 'const';
import { isServiceRunning } from './utils';

export function createWorker(values: any) {
  return defer(() => workerApi.create(values)).pipe(map((res) => res.data));
}

export function fetchWorker(values: ObjectKey) {
  return defer(() => workerApi.get(values)).pipe(map((res) => res.data));
}

// Attempt to start at intervals and wait until the service is not running
export function startWorker(key: ObjectKey) {
  return of(
    defer(() => workerApi.start(key)),
    defer(() => workerApi.get(key)).pipe(
      map((res) => {
        if (!isServiceRunning(res)) throw res;
        return res.data;
      }),
    ),
  ).pipe(
    concatAll(),
    last(),
    retryBackoff(RETRY_CONFIG),
    catchError((error) =>
      throwError({
        data: error?.data,
        meta: error?.meta,
        title:
          `Try to start worker: "${key.name}" failed after retry ${RETRY_CONFIG.maxRetries} times. ` +
          `Expected state: ${SERVICE_STATE.RUNNING}, Actual state: ${error.data.state}`,
      }),
    ),
  );
}

// Attempt to stop at intervals and wait until the service is not running
export function stopWorker(key: ObjectKey) {
  return of(
    defer(() => workerApi.stop(key)),
    defer(() => workerApi.get(key)).pipe(
      map((res) => {
        if (res.data?.state) throw res;
        return res.data;
      }),
    ),
  ).pipe(
    concatAll(),
    last(),
    retryBackoff(RETRY_CONFIG),
    catchError((error) =>
      throwError({
        data: error?.data,
        meta: error?.meta,
        title:
          `Try to stop worker: "${key.name}" failed after retry ${RETRY_CONFIG.maxRetries} times. ` +
          `Expected state is nonexistent, Actual state: ${error.data.state}`,
      }),
    ),
  );
}

export function deleteWorker(key: ObjectKey) {
  return defer(() => workerApi.remove(key));
}
