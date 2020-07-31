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

import {
  ObjectResponseList,
  ObjectResponse,
} from 'api/apiInterface/objectInterface';
import { of, Observable } from 'rxjs';
import { delay } from 'rxjs/operators';
import { ServiceBody } from 'api/apiInterface/clusterInterface';
import { BasicResponse } from 'api/apiInterface/basicInterface';

export const entity = {
  name: 'workspace1',
  group: 'workspace',
  nodeNames: ['node00'],
};

export const entities = [
  {
    name: 'workspace1',
    group: 'workspace',
    nodeNames: ['n1'],
  },
  {
    name: 'workspace2',
    group: 'workspace',
    nodeNames: ['n1', 'n2'],
  },
];

// simulate a promise request with a delay of 2s
export const create = (params: ServiceBody): Observable<ObjectResponse> =>
  of({
    status: 200,
    title: 'mock create workspace data',
    data: params,
  }).pipe(delay(100));

// simulate a promise request with a delay of 5 ms
export const getAll = (): Observable<ObjectResponseList> =>
  of({
    status: 200,
    title: 'mock workspace data',
    data: entities,
  }).pipe(delay(100));

// simulate a promise request with a delay of 1s
export const remove = (): Observable<BasicResponse> =>
  of({
    status: 200,
    title: 'mock delete workspace data',
    data: {},
  }).pipe(delay(100));

// simulate a promise request with a delay of 100ms
export const update = (params: ServiceBody): Observable<BasicResponse> =>
  of({
    status: 200,
    title: 'mock update workspace data',
    data: { ...entity, ...params },
  }).pipe(delay(100));
