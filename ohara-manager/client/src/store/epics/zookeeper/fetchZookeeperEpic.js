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

import { normalize } from 'normalizr';
import { merge } from 'lodash';
import { ofType } from 'redux-observable';
import { defer, forkJoin } from 'rxjs';
import { map, switchMap, startWith, throttleTime } from 'rxjs/operators';

import * as zookeeperApi from 'api/zookeeperApi';
import * as inspectApi from 'api/inspectApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';
import { catchErrorWithEventLog } from '../utils';

const fetchZookeeper$ = (params) => {
  const zookeeperId = getId(params);
  return forkJoin(
    defer(() => zookeeperApi.get(params)).pipe(
      map((res) => res.data),
      map((data) => normalize(data, schema.zookeeper)),
    ),
    defer(() => inspectApi.getZookeeperInfo(params)).pipe(
      map((res) => merge(res.data, params)),
      map((data) => normalize(data, schema.info)),
    ),
  ).pipe(
    map((normalizedData) => merge(...normalizedData, { zookeeperId })),
    map((normalizedData) => actions.fetchZookeeper.success(normalizedData)),
    startWith(actions.fetchZookeeper.request({ zookeeperId })),
    catchErrorWithEventLog((err) =>
      actions.fetchZookeeper.failure(merge(err, { zookeeperId })),
    ),
  );
};

export default (action$) =>
  action$.pipe(
    ofType(actions.fetchZookeeper.TRIGGER),
    map((action) => action.payload),
    throttleTime(1000),
    switchMap((params) => fetchZookeeper$(params)),
  );
