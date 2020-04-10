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

import { ObjectResponseList } from 'api/apiInterface/objectInterface';
import { of, Observable } from 'rxjs';
import { delay } from 'rxjs/operators';

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
// simulate a promise request with delay 5 ms
export const getAll = (): Observable<ObjectResponseList> =>
  of({
    status: 200,
    title: 'mock workspace data',
    data: entities,
  }).pipe(delay(5));
