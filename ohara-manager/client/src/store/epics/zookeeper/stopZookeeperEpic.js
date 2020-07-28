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
import { from } from 'rxjs';
import {
  catchError,
  map,
  startWith,
  distinctUntilChanged,
  mergeMap,
  takeUntil,
} from 'rxjs/operators';

import * as actions from 'store/actions';
import { stopZookeeper } from 'observables';
import * as schema from 'store/schema';
import { getId } from 'utils/object';
import { LOG_LEVEL } from 'const';

export default (action$) =>
  action$.pipe(
    ofType(actions.stopZookeeper.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap(({ values, resolve, reject }) => {
      const zookeeperId = getId(values);
      return stopZookeeper(values).pipe(
        map((data) => {
          if (resolve) resolve(data);
          const normalizedData = merge(normalize(data, schema.zookeeper), {
            zookeeperId,
          });
          return actions.stopZookeeper.success(normalizedData);
        }),
        startWith(actions.stopZookeeper.request({ zookeeperId })),
        catchError((err) => {
          if (reject) reject(err);
          return from([
            actions.stopZookeeper.failure(
              merge(err, { zookeeperId: getId(values) }),
            ),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]);
        }),
        takeUntil(action$.pipe(ofType(actions.stopZookeeper.CANCEL))),
      );
    }),
  );
