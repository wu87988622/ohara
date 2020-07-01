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
import { catchError, map, mergeMap } from 'rxjs/operators';
import { isEmpty, union, uniqWith } from 'lodash';
import { retryBackoff } from 'backoff-rxjs';

import { ObjectKey } from 'api/apiInterface/basicInterface';
import { SERVICE_STATE, ClusterData } from 'api/apiInterface/clusterInterface';
import * as streamApi from 'api/streamApi';
import { RETRY_CONFIG } from 'const';
import { fetchPipelines } from 'observables';
import { getKey, isEqualByKey } from 'utils/object';
import { hashByGroupAndName } from 'utils/sha';
import { isServiceRunning } from './utils';

export function createStream(values: any): Observable<ClusterData> {
  return defer(() => streamApi.create(values)).pipe(map((res) => res.data));
}

export function fetchStream(values: ObjectKey): Observable<ClusterData> {
  return defer(() => streamApi.get(values)).pipe(map((res) => res.data));
}

export function fetchStreams(
  pipelineKey: ObjectKey,
): Observable<ClusterData[]> {
  const streamGroup = hashByGroupAndName(pipelineKey.group, pipelineKey.name);
  return defer(() => streamApi.getAll({ group: streamGroup })).pipe(
    map((res) => res.data),
  );
}

export function startStream(key: ObjectKey): Observable<ClusterData> {
  return zip(
    // attempt to start at intervals
    defer(() => streamApi.start(key)),
    // wait until the service is running
    defer(() => streamApi.get(key)).pipe(
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
          `Try to start stream: "${key.name}" failed after retry ${RETRY_CONFIG.maxRetries} times. ` +
          `Expected state: ${SERVICE_STATE.RUNNING}, Actual state: ${error.data.state}`,
      }),
    ),
  );
}

export function stopStream(key: ObjectKey): Observable<ClusterData> {
  return zip(
    // attempt to stop at intervals
    defer(() => streamApi.stop(key)),
    // wait until the service is not running
    defer(() => streamApi.get(key)).pipe(
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
          `Try to stop stream: "${key.name}" failed after retry ${RETRY_CONFIG.maxRetries} times. ` +
          `Expected state is nonexistent, Actual state: ${error.data.state}`,
      }),
    ),
  );
}

export function deleteStream(key: ObjectKey) {
  return defer(() => streamApi.remove(key));
}

// Fetch and stop all streams for this workspace
export function fetchAndStopStreams(
  workspaceKey: ObjectKey,
): Observable<ClusterData[]> {
  return fetchPipelines(workspaceKey).pipe(
    mergeMap((pipelines) => {
      if (isEmpty(pipelines)) return of([]);
      return forkJoin(
        pipelines.map((pipeline) => {
          return fetchStreams(getKey(pipeline));
        }),
      );
    }),
    map((streamsArray: ClusterData[][]) => union(...streamsArray)),
    map((streams: ClusterData[]) => uniqWith(streams, isEqualByKey)),
    mergeMap((streams: ClusterData[]) => {
      if (isEmpty(streams)) return of(streams);
      return forkJoin(
        streams.map((stream: ClusterData) => stopStream(getKey(stream))),
      );
    }),
  );
}

// Fetch and delete all streams for this workspace
export function fetchAndDeleteStreams(workspaceKey: ObjectKey) {
  return fetchPipelines(workspaceKey).pipe(
    mergeMap((pipelines) => {
      if (isEmpty(pipelines)) return of([]);
      return forkJoin(
        pipelines.map((pipeline) => {
          return fetchStreams(getKey(pipeline));
        }),
      );
    }),
    map((streamsArray: ClusterData[][]) => union(...streamsArray)),
    map((streams: ClusterData[]) => uniqWith(streams, isEqualByKey)),
    mergeMap((streams: ClusterData[]) => {
      if (isEmpty(streams)) return of(streams);
      return forkJoin(
        streams.map((stream: ClusterData) => deleteStream(getKey(stream))),
      );
    }),
  );
}
