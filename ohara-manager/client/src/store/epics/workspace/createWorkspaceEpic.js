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
import { ofType } from 'redux-observable';
import { from } from 'rxjs';
import {
  catchError,
  distinctUntilChanged,
  map,
  mergeMap,
  startWith,
  takeUntil,
} from 'rxjs/operators';
import { merge } from 'lodash';

import { LOG_LEVEL } from 'const';
import { createWorkspace } from 'observables';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';

export default (action$) =>
  action$.pipe(
    ofType(actions.createWorkspace.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap(({ values, resolve, reject }) => {
      const workspaceId = getId(values);
      return createWorkspace(values).pipe(
        map((data) => {
          if (resolve) resolve(data);
          const normalizedData = merge(normalize(data, schema.workspace), {
            workspaceId,
          });
          return actions.createWorkspace.success(normalizedData);
        }),
        startWith(actions.createWorkspace.request({ workspaceId })),
        catchError((err) => {
          if (reject) reject(err);
          return from([
            actions.createWorkspace.failure(merge(err, { workspaceId })),
            actions.createEventLog.trigger({
              ...err,
              type: LOG_LEVEL.error,
            }),
          ]);
        }),
        takeUntil(action$.pipe(ofType(actions.createWorkspace.CANCEL))),
      );
    }),
  );
