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

import { merge } from 'lodash';
import { normalize } from 'normalizr';
import { ofType } from 'redux-observable';
import { defer, from, of } from 'rxjs';
import {
  catchError,
  distinctUntilChanged,
  map,
  mergeMap,
  startWith,
  switchMap,
  takeUntil,
} from 'rxjs/operators';

import * as zookeeperApi from 'api/zookeeperApi';
import { LOG_LEVEL, GROUP } from 'const';
import { createZookeeper } from 'observables';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';

export const createZookeeper$ = (values) => {
  const zookeeperId = getId(values);
  return defer(() => zookeeperApi.create(values)).pipe(
    map((res) => res.data),
    switchMap((data) => {
      const normalizedData = merge(normalize(data, schema.zookeeper), {
        zookeeperId,
      });
      return from([
        actions.updateWorkspace.trigger({
          zookeeper: data,
          // the workspace name of current object should be as same as the zookeeper name
          name: values.name,
          group: GROUP.WORKSPACE,
        }),
        actions.createZookeeper.success(normalizedData),
      ]);
    }),
    startWith(actions.createZookeeper.request({ zookeeperId })),
    catchError((err) =>
      from([
        actions.createZookeeper.failure(merge(err, { zookeeperId })),
        actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
      ]),
    ),
  );
};

export default (action$) =>
  action$.pipe(
    ofType(actions.createZookeeper.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap(({ values, resolve, reject }) => {
      const zookeeperId = getId(values);
      return createZookeeper(values).pipe(
        switchMap((data) => {
          if (resolve) resolve(data);
          const normalizedData = merge(normalize(data, schema.zookeeper), {
            zookeeperId,
          });
          return of(
            actions.updateWorkspace.trigger({
              values: {
                zookeeper: data,
                // the workspace name of current object should be as same as the zookeeper name
                name: values.name,
                group: GROUP.WORKSPACE,
              },
            }),
            actions.createZookeeper.success(normalizedData),
          );
        }),
        startWith(actions.createZookeeper.request({ zookeeperId })),
        catchError((err) => {
          if (reject) reject(err);
          return from([
            actions.createZookeeper.failure(merge(err, { zookeeperId })),
            actions.createEventLog.trigger({
              ...err,
              type: LOG_LEVEL.error,
            }),
          ]);
        }),
        takeUntil(action$.pipe(ofType(actions.createZookeeper.CANCEL))),
      );
    }),
  );
