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

import { PipelineResponseList } from 'api/apiInterface/pipelineInterface';
import { hashByGroupAndName } from 'utils/sha';
import { of, Observable } from 'rxjs';
import { delay } from 'rxjs/operators';

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
    name: 'p1',
    group: hashByGroupAndName('workspace', 'workspace2'),
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
];
// simulate a promise request with delay 6 ms
export const getAll = (): Observable<PipelineResponseList> =>
  of({
    status: 200,
    title: 'mock pipeline data',
    data: entities,
  }).pipe(delay(6));
