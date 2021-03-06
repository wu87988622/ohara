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
import { from } from 'rxjs';
import { catchError, map, mergeMap, startWith, tap } from 'rxjs/operators';

import { LOG_LEVEL } from 'const';
import { updateWorkspace } from 'observables';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';

export default (action$) =>
  action$.pipe(
    ofType(actions.updateWorkspace.TRIGGER),
    map((action) => action.payload),
    mergeMap(({ values, resolve, reject }) => {
      const workspaceId = getId(values);
      return updateWorkspace(values).pipe(
        tap((data) => resolve && resolve(data)),
        map((data) => normalize(data, schema.workspace)),
        map((normalizedData) => merge(normalizedData, { workspaceId })),
        map((normalizedData) =>
          actions.updateWorkspace.success(normalizedData),
        ),
        startWith(actions.updateWorkspace.request({ workspaceId })),
        catchError((err) => {
          if (reject) reject(err);
          return from([
            actions.updateWorkspace.failure(
              merge(err, { workspaceId: getId(values) }),
            ),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]);
        }),
      );
    }),
  );
