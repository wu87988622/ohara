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
import { defer, from } from 'rxjs';
import {
  catchError,
  distinctUntilChanged,
  map,
  mergeMap,
  switchMap,
  startWith,
  takeUntil,
} from 'rxjs/operators';

import * as brokerApi from 'api/brokerApi';
import { LOG_LEVEL, GROUP } from 'const';
import { createBroker } from 'observables';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';

export const createBroker$ = (values) => {
  const brokerId = getId(values);
  return defer(() => brokerApi.create(values)).pipe(
    map((res) => res.data),
    switchMap((data) => {
      const normalizedData = merge(normalize(data, schema.broker), {
        brokerId,
      });
      return from([
        actions.updateWorkspace.trigger({
          broker: data,
          // the workspace name of current object should be as same as the broker name
          name: values.name,
          group: GROUP.WORKSPACE,
        }),
        actions.createBroker.success(normalizedData),
      ]);
    }),
    startWith(actions.createBroker.request({ brokerId })),
    catchError((err) =>
      from([
        actions.createBroker.failure(merge(err, { brokerId })),
        actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
      ]),
    ),
  );
};

export default (action$) =>
  action$.pipe(
    ofType(actions.createBroker.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap(({ values, resolve, reject }) => {
      const brokerId = getId(values);
      return createBroker(values).pipe(
        switchMap((data) => {
          if (resolve) resolve(data);
          const normalizedData = merge(normalize(data, schema.broker), {
            brokerId,
          });
          return from([
            actions.updateWorkspace.trigger({
              broker: data,
              // the workspace name of current object should be as same as the broker name
              name: values.name,
              group: GROUP.WORKSPACE,
            }),
            actions.createBroker.success(normalizedData),
          ]);
        }),
        startWith(actions.createBroker.request({ brokerId })),
        catchError((err) => {
          if (reject) reject(err);
          return from([
            actions.createBroker.failure(merge(err, { brokerId })),
            actions.createEventLog.trigger({
              ...err,
              type: LOG_LEVEL.error,
            }),
          ]);
        }),
        takeUntil(action$.pipe(ofType(actions.createBroker.CANCEL))),
      );
    }),
  );
