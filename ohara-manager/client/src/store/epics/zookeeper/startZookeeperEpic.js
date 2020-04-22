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
import { defer, of, iif, throwError, zip } from 'rxjs';
import {
  catchError,
  delay,
  map,
  retryWhen,
  startWith,
  concatMap,
  mergeMap,
  distinctUntilChanged,
} from 'rxjs/operators';

import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import * as zookeeperApi from 'api/zookeeperApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';

export const startZookeeper$ = params => {
  const zookeeperId = getId(params);
  return zip(
    defer(() => zookeeperApi.start(params)),
    defer(() => zookeeperApi.get(params)).pipe(
      map(res => {
        if (!res.data?.state || res.data.state !== SERVICE_STATE.RUNNING)
          throw res;
        else return res.data;
      }),
      retryWhen(errors =>
        errors.pipe(
          concatMap((value, index) =>
            iif(
              () => index > 10,
              throwError('exceed max retry times'),
              of(value).pipe(delay(2000)),
            ),
          ),
        ),
      ),
    ),
  ).pipe(
    map(([, data]) => normalize(data, schema.zookeeper)),
    map(normalizedData => merge(normalizedData, { zookeeperId })),
    map(normalizedData => actions.startZookeeper.success(normalizedData)),
    startWith(actions.startZookeeper.request({ zookeeperId })),
    catchError(error =>
      of(actions.startZookeeper.failure(merge(error, { zookeeperId }))),
    ),
  );
};

export default action$ =>
  action$.pipe(
    ofType(actions.startZookeeper.TRIGGER),
    map(action => action.payload),
    distinctUntilChanged(),
    mergeMap(params => startZookeeper$(params)),
  );
