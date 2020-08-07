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
import { defer } from 'rxjs';
import { map, startWith, mergeMap, tap } from 'rxjs/operators';

import * as workerApi from 'api/workerApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';
import { catchErrorWithEventLog } from '../utils';

export const updateWorker$ = (values, resolve) => {
  const workerId = getId(values);
  return defer(() => workerApi.update(values)).pipe(
    map((res) => res.data),
    map((data) => normalize(data, schema.worker)),
    tap((data) => {
      if (resolve) resolve(data);
    }),
    map((normalizedData) => merge(normalizedData, { workerId })),
    map((normalizedData) => actions.updateWorker.success(normalizedData)),
    startWith(actions.updateWorker.request({ workerId })),
  );
};

export default (action$) =>
  action$.pipe(
    ofType(actions.updateWorker.TRIGGER),
    map((action) => action.payload),
    mergeMap(({ values, resolve, reject }) =>
      updateWorker$(values, resolve).pipe(
        catchErrorWithEventLog((err) => {
          if (reject) reject(err);
          return actions.updateWorker.failure(
            merge(err, { workerId: getId(values) }),
          );
        }),
      ),
    ),
  );
