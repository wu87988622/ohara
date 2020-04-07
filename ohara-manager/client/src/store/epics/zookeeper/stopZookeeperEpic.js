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
import { defer, interval, of } from 'rxjs';
import {
  catchError,
  concatAll,
  debounce,
  delay,
  map,
  retryWhen,
  startWith,
  switchMap,
  take,
} from 'rxjs/operators';

import * as zookeeperApi from 'api/zookeeperApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';

export const stopZookeeper$ = params => {
  const zookeeperId = getId(params);
  return of(
    defer(() => zookeeperApi.stop(params)),
    defer(() => zookeeperApi.get(params)).pipe(
      map(res => {
        if (res.data?.state) throw res;
        return res.data;
      }),
      map(data => normalize(data, schema.zookeeper)),
      map(normalizedData => merge(normalizedData, { zookeeperId })),
      map(normalizedData => actions.stopZookeeper.success(normalizedData)),
      retryWhen(error => error.pipe(delay(2000), take(10))),
    ),
  ).pipe(
    concatAll(),
    startWith(actions.stopZookeeper.request({ zookeeperId })),
    catchError(error =>
      of(actions.stopZookeeper.failure(merge(error, { zookeeperId }))),
    ),
  );
};

export default action$ =>
  action$.pipe(
    ofType(actions.stopZookeeper.TRIGGER),
    map(action => action.payload),
    debounce(() => interval(1000)),
    switchMap(params => stopZookeeper$(params)),
  );
