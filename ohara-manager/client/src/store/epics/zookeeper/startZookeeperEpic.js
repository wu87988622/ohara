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

import { defer, from, iif, of, throwError, zip } from 'rxjs';
import {
  catchError,
  concatMap,
  delay,
  distinctUntilChanged,
  map,
  mergeMap,
  retryWhen,
  startWith,
  takeUntil,
} from 'rxjs/operators';
import { ofType } from 'redux-observable';
import { normalize } from 'normalizr';
import { merge } from 'lodash';

import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import * as zookeeperApi from 'api/zookeeperApi';
import { LOG_LEVEL } from 'const';
import * as actions from 'store/actions';
import { startZookeeper } from 'observables';
import * as schema from 'store/schema';
import { getId } from 'utils/object';

// Note: The caller SHOULD handle the error of this action
export const startZookeeper$ = (params) => {
  const zookeeperId = getId(params);
  return zip(
    defer(() => zookeeperApi.start(params)),
    defer(() => zookeeperApi.get(params)).pipe(
      map((res) => {
        if (!res.data?.state || res.data.state !== SERVICE_STATE.RUNNING)
          throw res;
        else return res.data;
      }),
    ),
  ).pipe(
    retryWhen((errors) =>
      errors.pipe(
        concatMap((value, index) =>
          iif(
            () => index > 10,
            throwError({
              data: value?.data,
              meta: value?.meta,
              title:
                `Try to start zookeeper: "${params.name}" failed after retry ${index} times. ` +
                `Expected state: ${SERVICE_STATE.RUNNING}, Actual state: ${value.data.state}`,
            }),
            of(value).pipe(delay(2000)),
          ),
        ),
      ),
    ),
    map(([, data]) => normalize(data, schema.zookeeper)),
    map((normalizedData) => merge(normalizedData, { zookeeperId })),
    map((normalizedData) => actions.startZookeeper.success(normalizedData)),
    startWith(actions.startZookeeper.request({ zookeeperId })),
  );
};

export default (action$) =>
  action$.pipe(
    ofType(actions.startZookeeper.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap(({ values, resolve, reject }) => {
      const zookeeperId = getId(values);
      return startZookeeper(values).pipe(
        map((data) => {
          if (resolve) resolve(data);
          const normalizedData = merge(normalize(data, schema.zookeeper), {
            zookeeperId,
          });
          return actions.startZookeeper.success(normalizedData);
        }),
        startWith(actions.startZookeeper.request({ zookeeperId })),
        catchError((err) => {
          if (reject) reject(err);
          return from([
            actions.startZookeeper.failure(
              merge(err, { zookeeperId: getId(values) }),
            ),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]);
        }),
        takeUntil(action$.pipe(ofType(actions.startZookeeper.CANCEL))),
      );
    }),
  );
