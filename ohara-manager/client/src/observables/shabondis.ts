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

import { Observable, defer, forkJoin, of, throwError, zip } from 'rxjs';
import { catchError, map, mapTo, mergeMap } from 'rxjs/operators';
import { isEmpty, union, uniqWith } from 'lodash';
import { retryBackoff } from 'backoff-rxjs';

import { ObjectKey } from 'api/apiInterface/basicInterface';
import { SERVICE_STATE, ClusterData } from 'api/apiInterface/clusterInterface';
import * as shabondiApi from 'api/shabondiApi';
import { RETRY_CONFIG } from 'const';
import { fetchPipelines } from 'observables';
import { getKey, isEqualByKey } from 'utils/object';
import { hashByGroupAndName } from 'utils/sha';
import { isServiceRunning } from './utils';

export function createShabondi(values: any): Observable<ClusterData> {
  return defer(() => shabondiApi.create(values)).pipe(map((res) => res.data));
}

export function fetchShabondi(values: ObjectKey): Observable<ClusterData> {
  return defer(() => shabondiApi.get(values)).pipe(map((res) => res.data));
}

export function fetchShabondis(
  pipelineKey: ObjectKey,
): Observable<ClusterData[]> {
  const shabondiGroup = hashByGroupAndName(pipelineKey.group, pipelineKey.name);
  return defer(() => shabondiApi.getAll({ group: shabondiGroup })).pipe(
    map((res) => res.data),
  );
}

export function startShabondi(key: ObjectKey): Observable<ClusterData> {
  return zip(
    // attempt to start at intervals
    defer(() => shabondiApi.start(key)),
    // wait until the service is running
    defer(() => shabondiApi.get(key)).pipe(
      map((res) => {
        if (!isServiceRunning(res)) throw res;
        return res.data;
      }),
    ),
  ).pipe(
    map(([, data]) => data),
    // retry every 2 seconds, up to 10 times
    retryBackoff(RETRY_CONFIG),
    catchError((error) =>
      throwError({
        data: error?.data,
        meta: error?.meta,
        title:
          `Try to start shabondi: "${key.name}" failed after retry ${RETRY_CONFIG.maxRetries} times. ` +
          `Expected state: ${SERVICE_STATE.RUNNING}, Actual state: ${error.data.state}`,
      }),
    ),
  );
}

export function deleteShabondi(key: ObjectKey) {
  return defer(() => shabondiApi.remove(key));
}

export function stopShabondi(key: ObjectKey): Observable<ClusterData> {
  return zip(
    // attempt to stop at intervals
    defer(() => shabondiApi.stop(key)),
    // wait until the service is not running
    defer(() => shabondiApi.get(key)).pipe(
      map((res) => {
        if (res.data?.state) throw res;
        return res.data;
      }),
    ),
  ).pipe(
    map(([, data]) => data),
    // retry every 2 seconds, up to 10 times
    retryBackoff(RETRY_CONFIG),
    catchError((error) =>
      throwError({
        data: error?.data,
        meta: error?.meta,
        title:
          `Try to stop shabondi: "${key.name}" failed after retry ${RETRY_CONFIG.maxRetries} times. ` +
          `Expected state is nonexistent, Actual state: ${error.data.state}`,
      }),
    ),
  );
}

// Fetch and stop all shabondis for this workspace
export function fetchAndStopShabondis(
  workspaceKey: ObjectKey,
): Observable<ClusterData[]> {
  return fetchPipelines(workspaceKey).pipe(
    mergeMap((pipelines) => {
      if (isEmpty(pipelines)) return of([]);
      return forkJoin(
        pipelines.map((pipeline) => {
          return fetchShabondis(getKey(pipeline));
        }),
      );
    }),
    map((shabondisArray: ClusterData[][]) => union(...shabondisArray)),
    map((shabondis: ClusterData[]) => uniqWith(shabondis, isEqualByKey)),
    mergeMap((shabondis: ClusterData[]) => {
      if (isEmpty(shabondis)) return of(shabondis);
      return forkJoin(
        shabondis.map((shabondi: ClusterData) =>
          stopShabondi(getKey(shabondi)),
        ),
      );
    }),
  );
}

// Fetch and delete all shabondis for this workspace
export function fetchAndDeleteShabondis(workspaceKey: ObjectKey) {
  return fetchPipelines(workspaceKey).pipe(
    mergeMap((pipelines) => {
      if (isEmpty(pipelines)) return of([]);
      return forkJoin(
        pipelines.map((pipeline) => {
          return fetchShabondis(getKey(pipeline));
        }),
      );
    }),
    map((shabondisArray) => union(...shabondisArray)),
    map((shabondis) => uniqWith(shabondis, isEqualByKey)),
    mergeMap((shabondis) => {
      if (isEmpty(shabondis)) return of(shabondis);
      return forkJoin(
        shabondis.map((shabondi) =>
          deleteShabondi(getKey(shabondi)).pipe(mapTo(shabondi)),
        ),
      );
    }),
  );
}
