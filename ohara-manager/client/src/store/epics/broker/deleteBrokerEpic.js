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
import { from } from 'rxjs';
import {
  catchError,
  map,
  startWith,
  mergeMap,
  distinctUntilChanged,
  takeUntil,
} from 'rxjs/operators';

import { deleteBroker } from 'observables';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { LOG_LEVEL } from 'const';

export default (action$) =>
  action$.pipe(
    ofType(actions.deleteBroker.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap(({ values, resolve, reject }) => {
      const brokerId = getId(values);
      return deleteBroker(values).pipe(
        map(() => {
          if (resolve) resolve();
          return actions.deleteBroker.success({ brokerId });
        }),
        startWith(actions.deleteBroker.request({ brokerId })),
        catchError((err) => {
          if (reject) reject(err);
          return from([
            actions.deleteBroker.failure(merge(err, { brokerId })),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]);
        }),
        takeUntil(action$.pipe(ofType(actions.deleteBroker.CANCEL))),
      );
    }),
  );
