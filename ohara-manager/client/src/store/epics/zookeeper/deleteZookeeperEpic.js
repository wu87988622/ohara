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
import { ofType } from 'redux-observable';
import { defer, from } from 'rxjs';
import {
  catchError,
  distinctUntilChanged,
  map,
  mergeMap,
  startWith,
  takeUntil,
} from 'rxjs/operators';

import { LOG_LEVEL } from 'const';
import * as zookeeperApi from 'api/zookeeperApi';
import { deleteZookeeper } from 'observables';
import * as actions from 'store/actions';
import { getId } from 'utils/object';

// Note: The caller SHOULD handle the error of this action
export const deleteZookeeper$ = (params) => {
  const zookeeperId = getId(params);
  return defer(() => zookeeperApi.remove(params)).pipe(
    map(() => actions.deleteZookeeper.success({ zookeeperId })),
    startWith(actions.deleteZookeeper.request({ zookeeperId })),
  );
};

export default (action$) =>
  action$.pipe(
    ofType(actions.deleteZookeeper.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap(({ values, resolve, reject }) => {
      const zookeeperId = getId(values);
      return deleteZookeeper(values).pipe(
        map(() => {
          if (resolve) resolve();
          return actions.deleteZookeeper.success({ zookeeperId });
        }),
        startWith(actions.deleteZookeeper.request({ zookeeperId })),
        catchError((err) => {
          if (reject) reject(err);
          return from([
            actions.deleteZookeeper.failure(merge(err, { zookeeperId })),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]);
        }),
        takeUntil(action$.pipe(ofType(actions.deleteZookeeper.CANCEL))),
      );
    }),
  );
