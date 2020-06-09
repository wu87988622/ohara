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
import { delay } from 'rxjs/operators';

import {
  PipelineResponseList,
  PipelineRequest,
  PipelineResponse,
} from 'api/apiInterface/pipelineInterface';
import { BasicResponse } from 'api/apiInterface/basicInterface';
import { hashByGroupAndName } from 'utils/sha';

export const entity = {
  name: 'pp',
  group: hashByGroupAndName('workspace', 'workspace1'),
  endpoints: [],
  objects: [],
  jarKeys: [],
  lastModified: 0,
  tags: {},
};

export const entities = [
  {
    name: 'p1',
    group: hashByGroupAndName('workspace', 'workspace1'),
    endpoints: [],
    objects: [],
    jarKeys: [],
    lastModified: 0,
    tags: {},
  },
  {
    name: 'p2',
    group: hashByGroupAndName('workspace', 'workspace2'),
    endpoints: [],
    objects: [],
    jarKeys: [],
    lastModified: 0,
    tags: {},
  },
  {
    name: 'p3',
    group: hashByGroupAndName('workspace', 'workspace3'),
    endpoints: [],
    objects: [],
    jarKeys: [],
    lastModified: 0,
    tags: {},
  },
];

export const get = (): Observable<PipelineResponse> =>
  of({
    status: 200,
    title: 'Get pipeline mock',
    data: entity,
  }).pipe(delay(6));

// simulate a promise request with a delay of 6 ms
export const getAll = (): Observable<PipelineResponseList> =>
  of({
    status: 200,
    title: 'Get all pipelines mock',
    data: entities,
  }).pipe(delay(6));

export const create = (params: PipelineRequest): Observable<PipelineResponse> =>
  of({
    status: 200,
    title: 'Create pipeline mock',
    data: { ...entity, ...params },
  }).pipe(delay(2000));

export const remove = (): Observable<BasicResponse> =>
  of({
    status: 200,
    title: 'Delete pipeline mock',
    data: {},
  }).pipe(delay(500));

export const update = (params: PipelineRequest): Observable<PipelineResponse> =>
  of({
    status: 200,
    title: 'Update pipeline mock',
    data: { ...entity, ...params },
  }).pipe(delay(100));
