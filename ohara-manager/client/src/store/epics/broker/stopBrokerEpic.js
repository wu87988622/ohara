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
import { defer, of, iif, throwError } from 'rxjs';
import {
  catchError,
  concatAll,
  delay,
  map,
  retryWhen,
  startWith,
  distinctUntilChanged,
  mergeMap,
  concatMap,
} from 'rxjs/operators';

import * as brokerApi from 'api/brokerApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';

const stopBroker$ = params => {
  const brokerId = getId(params);
  return of(
    defer(() => brokerApi.stop(params)),
    defer(() => brokerApi.get(params)).pipe(
      map(res => {
        if (res.data?.state) throw res;
        else return res.data;
      }),
      map(data => normalize(data, schema.broker)),
      map(normalizedData => merge(normalizedData, { brokerId })),
      map(normalizedData => actions.stopBroker.success(normalizedData)),
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
    concatAll(),
    startWith(actions.stopBroker.request({ brokerId })),
    catchError(error =>
      of(actions.stopBroker.failure(merge(error, { brokerId }))),
    ),
  );
};

export default action$ =>
  action$.pipe(
    ofType(actions.stopBroker.TRIGGER),
    map(action => action.payload),
    distinctUntilChanged(),
    mergeMap(params => stopBroker$(params)),
  );
