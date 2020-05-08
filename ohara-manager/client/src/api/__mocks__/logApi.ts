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
import { ObjectKey } from 'api/apiInterface/basicInterface';
import { LogResponse } from 'api/apiInterface/logInterface';
import { getId } from 'utils/object';

export const makeLog = (key?: ObjectKey) =>
  `fake ${getId(key)} log\nanother fake log\nagain, again and again.`;
const getLog = (key?: ObjectKey) => ({
  clusterKey: {
    group: key?.group || '',
    name: key?.name || '',
  },
  logs: [
    {
      hostname: 'node01',
      value: makeLog(key),
    },
    {
      hostname: 'node02',
      value: makeLog(key),
    },
  ],
});

// simulate a promise request with a delay of 100ms
export const getConfiguratorLog = (): Observable<LogResponse> =>
  of({
    status: 200,
    title: 'mock configurator log data',
    data: getLog(),
  }).pipe(delay(100));

// simulate a promise request with a delay of 100ms
export const getZookeeperLog = (key: ObjectKey): Observable<LogResponse> =>
  of({
    status: 200,
    title: 'mock zookeeper log data',
    data: getLog(key),
  }).pipe(delay(100));

// simulate a promise request with a delay of 100ms
export const getBrokerLog = (key: ObjectKey): Observable<LogResponse> =>
  of({
    status: 200,
    title: 'mock broker log data',
    data: getLog(key),
  }).pipe(delay(100));

// simulate a promise request with a delay of 100ms
export const getWorkerLog = (key: ObjectKey): Observable<LogResponse> =>
  of({
    status: 200,
    title: 'mock worker log data',
    data: getLog(key),
  }).pipe(delay(100));

// simulate a promise request with a delay of 100ms
export const getShabondiLog = (key: ObjectKey): Observable<LogResponse> =>
  of({
    status: 200,
    title: 'mock shabondi log data',
    data: getLog(key),
  }).pipe(delay(100));

// simulate a promise request with a delay of 100ms
export const getStreamLog = (key: ObjectKey): Observable<LogResponse> =>
  of({
    status: 200,
    title: 'mock stream log data',
    data: getLog(key),
  }).pipe(delay(100));
