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
import * as zookeeperApi from 'api/zookeeperApi';
import { deferApi } from './internal/deferApi';
import { isServiceRunning } from './utils';

export function createZookeeper(values: any) {
  return deferApi(() => zookeeperApi.create(values)).pipe(
    map((res) => res.data),
  );
}

export function fetchZookeeper(key: ObjectKey) {
  return deferApi(() => zookeeperApi.get(key)).pipe(map((res) => res.data));
}

export function startZookeeper(key: ObjectKey) {
  return zip(
    // attempt to start at intervals
    deferApi(() => zookeeperApi.start(key)).pipe(),
    // wait until the service is running
    deferApi(() => zookeeperApi.get(key)).pipe(
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
          `Try to start zookeeper: "${key.name}" failed after retry 10 times. ` +
          `Expected state: ${SERVICE_STATE.RUNNING}, Actual state: ${error.data.state}`,
      }),
    ),
  );
}
