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
import { BasicResponse } from 'api/apiInterface/basicInterface';
import {
  FileResponse,
  FileRequest,
  FileResponseList,
} from 'api/apiInterface/fileInterface';

export const entity = {
  name: 'ohara-it-stream.jar',
  size: 1896,
  url: 'http://localhost:12345/v0/downloadFiles/default/ohara-it-stream.jar',
  lastModified: 1578967196525,
  tags: {},
  classInfos: [],
  group: 'default',
  file: {
    type: 'jar',
  },
};

// simulate a promise request with a delay of 2s
export const create = (params: FileRequest): Observable<FileResponse> =>
  of({
    status: 200,
    title: 'mock create file data',
    data: { ...entity, ...params },
  }).pipe(delay(2000));

// simulate a promise request with a delay of 100ms
export const update = (params: {
  [k: string]: any;
}): Observable<FileResponse> =>
  of({
    status: 200,
    title: 'mock update file data',
    data: { ...entity, ...params },
  }).pipe(delay(100));

// simulate a promise request with a delay of 1s
export const remove = (): Observable<BasicResponse> =>
  of({
    status: 200,
    title: 'mock delete file data',
    data: {},
  }).pipe(delay(1000));

// simulate a promise request with a delay of 500ms
export const get = (): Observable<FileResponse> =>
  of({
    status: 200,
    title: 'mock get file data',
    data: entity,
  }).pipe(delay(500));

// simulate a promise request with a delay of 500ms
export const getAll = (): Observable<FileResponseList> =>
  of({
    status: 200,
    title: 'mock get file list data',
    data: [entity],
  }).pipe(delay(500));
