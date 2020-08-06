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

import {
  distinctUntilChanged,
  map,
  mergeMap,
  startWith,
  takeUntil,
} from 'rxjs/operators';
import { ofType } from 'redux-observable';
import { normalize } from 'normalizr';
import { merge } from 'lodash';

import * as actions from 'store/actions';
import { startZookeeper } from 'observables';
import * as schema from 'store/schema';
import { getId } from 'utils/object';
import { catchErrorWithEventLog } from '../utils';

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
        catchErrorWithEventLog((err) => {
          if (reject) reject(err);
          return actions.startZookeeper.failure(
            merge(err, { zookeeperId: getId(values) }),
          );
        }),
        takeUntil(action$.pipe(ofType(actions.startZookeeper.CANCEL))),
      );
    }),
  );
