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
  VolumeResponse,
  VolumeResponseList,
  VolumeData,
} from 'api/apiInterface/volumeInterface';
import { ServiceBody, SERVICE_STATE } from 'api/apiInterface/clusterInterface';

export let entity: VolumeData = {
  name: 'volume1',
  group: 'volume',
  lastModified: 0,
  nodeNames: ['node00'],
  path: '/home/ohara/data',
  tags: {
    displayName: 'volume1',
    workspaceName: 'workspace1',
  },
};

// simulate a promise request with a delay of 2s
export const create = (params: ServiceBody): Observable<VolumeResponse> =>
  of({
    status: 200,
    title: 'mock create volume data',
    data: { ...entity, ...params },
  }).pipe(delay(100));

// simulate a promise request with a delay of 100ms
export const update = (params: {
  [k: string]: any;
}): Observable<VolumeResponse> =>
  of({
    status: 200,
    title: 'mock update volume data',
    data: { ...entity, ...params },
  }).pipe(delay(100));

// simulate a promise request with a delay of 1s
export const remove = (): Observable<BasicResponse> =>
  of({
    status: 200,
    title: 'mock delete volume data',
    data: {},
  }).pipe(delay(100));

// simulate a promise request with a delay of 500ms
export const get = (): Observable<VolumeResponse> =>
  of({
    status: 200,
    title: 'mock get volume data',
    data: entity,
  }).pipe(delay(100));

// simulate a promise request with a delay of 500ms
export const getAll = (): Observable<VolumeResponseList> =>
  of({
    status: 200,
    title: 'mock get volume list data',
    data: [entity],
  }).pipe(delay(100));

// simulate a promise request with a delay of 10ms
export const start = (): Observable<BasicResponse> =>
  of({
    status: 200,
    title: 'mock start volume data',
    data: entity,
  }).pipe(
    // to simulate volume is "started" in fetch request
    tap(() => (entity = { ...entity, state: SERVICE_STATE.RUNNING })),
    delay(100),
  );

// simulate a promise request with a delay of 10ms
export const stop = (): Observable<BasicResponse> =>
  of({
    status: 200,
    title: 'mock stop volume data',
    data: entity,
  }).pipe(
    // to simulate volume is "stopped" in fetch request
    tap(() => delete entity.state),
    delay(100),
  );
