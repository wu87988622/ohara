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

import { omit } from 'lodash';
import { of, Observable } from 'rxjs';
import { delay, tap } from 'rxjs/operators';
import {
  ClusterData,
  ClusterResponse,
  ServiceBody,
  SERVICE_STATE,
  ClusterResponseList,
} from 'api/apiInterface/clusterInterface';
import { ObjectKey, BasicResponse } from 'api/apiInterface/basicInterface';

export const entity = {
  brokerClusterKey: {
    group: 'default',
    name: 'bk',
  },
  name: 'topic0',
  partitionInfos: [],
  lastModified: 0,
  tags: {},
  numberOfReplications: 1,
  group: 'default',
  numberOfPartitions: 1,
  aliveNodes: [],
  nodeMetrics: {},
};

export const entities = [
  entity,
  { ...entity, name: 'topic1' },
  { ...entity, name: 'topic2' },
];

let runtimeData: ClusterData = {
  aliveNodes: [],
  lastModified: 0,
  nodeMetrics: {},
};

// simulate a promise request with a delay of 2s
export const create = (params: ServiceBody): Observable<ClusterResponse> =>
  of({
    status: 200,
    title: 'mock create topic data',
    data: { ...runtimeData, ...params },
  }).pipe(delay(100));

// simulate a promise request with a delay of 1s
export const remove = (): Observable<BasicResponse> =>
  of({
    status: 200,
    title: 'mock delete topic data',
    data: {},
  }).pipe(delay(100));

// simulate a promise request with a delay of 500ms
export const get = (params: ObjectKey): Observable<ClusterResponse> =>
  of({
    status: 200,
    title: 'mock get topic data',
    data: { ...entity, ...runtimeData, ...params },
  }).pipe(delay(100));

// simulate a promise request with a delay of 500ms
export const getAll = (): Observable<ClusterResponseList> =>
  of({
    status: 200,
    title: 'mock get topic list data',
    data: [{ ...entity, ...runtimeData }],
  }).pipe(delay(100));

// simulate a promise request with a delay of 100ms
export const update = (params: ServiceBody): Observable<ClusterResponse> =>
  of({
    status: 200,
    title: 'mock update topic data',
    data: { ...runtimeData, ...params },
  }).pipe(delay(100));

// simulate a promise request with a delay of 10ms
export const start = (): Observable<BasicResponse> =>
  of({
    status: 200,
    title: 'mock start topic data',
    data: { ...omit(runtimeData, 'state') },
  }).pipe(
    // to simulate topic is "started" in fetch request
    tap(() => (runtimeData = { ...runtimeData, state: SERVICE_STATE.RUNNING })),
    delay(100),
  );

// simulate a promise request with a delay of 10ms
export const stop = (): Observable<BasicResponse> =>
  of({
    status: 200,
    title: 'mock stop topic data',
    data: { ...omit(runtimeData, 'state') },
  }).pipe(
    // to simulate topic is "stopped" in fetch request
    tap(() => delete runtimeData.state),
    delay(100),
  );
