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
import {
  State,
  Data as ConnectorData,
} from 'api/apiInterface/connectorInterface';
import * as connectorApi from 'api/connectorApi';
import { RETRY_CONFIG } from 'const';
import { fetchPipelines } from 'observables';
import { hashByGroupAndName } from 'utils/sha';
import { getKey, isEqualByKey } from 'utils/object';

export function createConnector(values: any) {
  return defer(() => connectorApi.create(values)).pipe(map((res) => res.data));
}

export function fetchConnector(values: ObjectKey) {
  return defer(() => connectorApi.get(values)).pipe(map((res) => res.data));
}

export function fetchConnectors(
  pipelineKey: ObjectKey,
): Observable<ConnectorData[]> {
  const connectorGroup = hashByGroupAndName(
    pipelineKey.group,
    pipelineKey.name,
  );
  return defer(() => connectorApi.getAll({ group: connectorGroup })).pipe(
    map((res) => res.data),
  );
}

export function startConnector(key: ObjectKey) {
  return zip(
    // attempt to start at intervals
    defer(() => connectorApi.start(key)),
    // wait until the service is running
    defer(() => connectorApi.get(key)).pipe(
      map((res: any) => {
        if (res?.data?.state === State.RUNNING) return res.data;
        throw res;
      }),
    ),
  ).pipe(
    map(([, data]) => data),
    retryBackoff(RETRY_CONFIG),
    catchError((error) =>
      throwError({
        data: error?.data,
        meta: error?.meta,
        title:
          `Try to start connector: "${key.name}" failed after retry ${RETRY_CONFIG.maxRetries} times. ` +
          `Expected state: ${State.RUNNING}, Actual state: ${error.data.state}`,
      }),
    ),
  );
}

export function stopConnector(key: ObjectKey) {
  return zip(
    // attempt to stop at intervals
    defer(() => connectorApi.stop(key)),
    // wait until the service is not running
    defer(() => connectorApi.get(key)).pipe(
      map((res: any) => {
        if (!res?.data?.state) return res.data;
        throw res;
      }),
    ),
  ).pipe(
    map(([, data]) => data),
    retryBackoff(RETRY_CONFIG),
    catchError((error) =>
      throwError({
        data: error?.data,
        meta: error?.meta,
        title:
          `Try to stop connector: "${key.name}" failed after retry ${RETRY_CONFIG.maxRetries} times. ` +
          `Expected state is nonexistent, Actual state: ${error.data.state}`,
      }),
    ),
  );
}

export function deleteConnector(key: ObjectKey) {
  return defer(() => connectorApi.remove(key));
}

// Fetch and stop all connectors for this workspace
export function fetchAndStopConnectors(
  workspaceKey: ObjectKey,
): Observable<ConnectorData[]> {
  return fetchPipelines(workspaceKey).pipe(
    mergeMap((pipelines) => {
      if (isEmpty(pipelines)) return of([]);
      return forkJoin(
        pipelines.map((pipeline) => fetchConnectors(getKey(pipeline))),
      );
    }),
    map((connectorsArray: ConnectorData[][]) => union(...connectorsArray)),
    map((connectors: ConnectorData[]) => uniqWith(connectors, isEqualByKey)),
    mergeMap((connectors: ConnectorData[]) => {
      if (isEmpty(connectors)) return of(connectors);
      return forkJoin(
        connectors.map((connector: ConnectorData) =>
          stopConnector(getKey(connector)),
        ),
      );
    }),
  );
}

// Fetch and delete all connectors for this workspace
export function fetchAndDeleteConnectors(workspaceKey: ObjectKey) {
  return fetchPipelines(workspaceKey).pipe(
    mergeMap((pipelines) => {
      if (isEmpty(pipelines)) return of([]);
      return forkJoin(
        pipelines.map((pipeline) => {
          return fetchConnectors(getKey(pipeline));
        }),
      );
    }),
    map((connectorsArray) => union(...connectorsArray)),
    map((connectors) => uniqWith(connectors, isEqualByKey)),
    mergeMap((connectors) => {
      if (isEmpty(connectors)) return of(connectors);
      return forkJoin(
        connectors.map((connector) =>
          deleteConnector(getKey(connector)).pipe(mapTo(connector)),
        ),
      );
    }),
  );
}
