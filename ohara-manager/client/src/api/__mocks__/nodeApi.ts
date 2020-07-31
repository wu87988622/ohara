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

import { of, Observable } from 'rxjs';
import { delay, tap } from 'rxjs/operators';

import { BasicResponse } from 'api/apiInterface/basicInterface';
import {
  NODE_STATE,
  NodeResponse,
  NodeRequest,
  NodeResponseList,
} from 'api/apiInterface/nodeInterface';

export let entity = {
  services: [
    {
      name: 'zookeeper',
      clusterKeys: [],
    },
    {
      name: 'broker',
      clusterKeys: [],
    },
    {
      name: 'connect-worker',
      clusterKeys: [],
    },
    {
      name: 'stream',
      clusterKeys: [],
    },
    {
      name: 'configurator',
      clusterKeys: [
        {
          group: 'N/A',
          name: 'node00',
        },
      ],
    },
  ],
  hostname: 'node00',
  state: NODE_STATE.AVAILABLE,
  lastModified: 1578627668686,
  tags: {},
  port: 22,
  resources: [
    {
      name: 'CPU',
      value: 6.0,
      unit: 'cores',
    },
    {
      name: 'Memory',
      value: 10.496479034423828,
      unit: 'GB',
    },
  ],
  user: 'abc',
  password: 'pwd',
};

// simulate a promise request with a delay of 2s
export const create = (params: NodeRequest): Observable<NodeResponse> =>
  of({
    status: 200,
    title: 'mock create node data',
    data: { ...entity, ...params },
  }).pipe(delay(100));

// simulate a promise request with a delay of 100ms
export const update = (params: NodeRequest): Observable<NodeResponse> =>
  of({
    status: 200,
    title: 'mock update node data',
    data: { ...entity, ...params },
  }).pipe(delay(100));

// simulate a promise request with a delay of 5 ms
export const getAll = (): Observable<NodeResponseList> =>
  of({
    status: 200,
    title: 'mock node data',
    data: [entity],
  }).pipe(delay(100));

// simulate a promise request with a delay of 5 ms
export const get = (): Observable<NodeResponse> =>
  of({
    status: 200,
    title: 'mock node data',
    data: entity,
  }).pipe(delay(100));

// simulate a promise request with a delay of 500ms
export const remove = (): Observable<BasicResponse> =>
  of({
    status: 200,
    title: 'mock delete node data',
    data: {},
  }).pipe(
    tap(() => (entity.hostname = 'unexistednode')),
    delay(100),
  );
