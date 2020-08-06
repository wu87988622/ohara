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
import {
  map,
  startWith,
  distinctUntilChanged,
  mergeMap,
  takeUntil,
} from 'rxjs/operators';

import * as actions from 'store/actions';
import { stopBroker } from 'observables';
import * as schema from 'store/schema';
import { getId } from 'utils/object';
import { catchErrorWithEventLog } from '../utils';

export default (action$) =>
  action$.pipe(
    ofType(actions.stopBroker.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap(({ values, resolve, reject }) => {
      const brokerId = getId(values);
      return stopBroker(values).pipe(
        map((data) => {
          if (resolve) resolve(data);
          const normalizedData = merge(normalize(data, schema.broker), {
            brokerId,
          });
          return actions.stopBroker.success(normalizedData);
        }),
        startWith(actions.stopBroker.request({ brokerId })),
        catchErrorWithEventLog((err) => {
          if (reject) reject(err);
          return actions.stopBroker.failure(
            merge(err, { brokerId: getId(values) }),
          );
        }),
        takeUntil(action$.pipe(ofType(actions.stopBroker.CANCEL))),
      );
    }),
  );
