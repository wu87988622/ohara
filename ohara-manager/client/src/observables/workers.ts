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

import { zip } from 'rxjs';
import { map } from 'rxjs/operators';
import { ObjectKey } from 'api/apiInterface/basicInterface';
import * as workerApi from 'api/workerApi';
import { deferApi } from './internal/deferApi';
import { retry } from './internal/retry';
import { linearDelayStrategy } from './internal/strategies';
import { isServiceRunning } from './utils';

export function getWorker(values: ObjectKey) {
  return deferApi(() => workerApi.get(values)).pipe(map((res) => res.data));
}

export function startWorker(
  values: ObjectKey,
  waitUtilStarted: boolean = false,
) {
  if (waitUtilStarted) {
    return zip(
      deferApi(() => workerApi.start(values)),
      checkWorkerRunning(values),
    ).pipe(
      map(([_, isRunning]) => {
        if (!isRunning) {
          throw {
            meta: { status: -1 },
            title: 'Failed to start worker',
          };
        }
        return true;
      }),
    );
  }

  return deferApi(() => workerApi.start(values));
}

export function checkWorkerRunning(values: ObjectKey) {
  return getWorker(values).pipe(
    map((data) => {
      if (isServiceRunning(data)) return true;
      throw false;
    }),
    retry({
      delay: 2000,
      maxRetries: 10,
      delayStrategy: linearDelayStrategy,
    }),
  );
}
