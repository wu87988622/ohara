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
  distinctUntilChanged,
  map,
  mergeMap,
  startWith,
  takeUntil,
  tap,
} from 'rxjs/operators';

import { LOG_LEVEL } from 'const';
import { deleteWorkspace } from 'observables';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { catchErrorWithEventLog } from '../utils';

export default (action$) =>
  action$.pipe(
    ofType(actions.deleteWorkspace.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap(({ values, resolve, reject }) => {
      const { workspaceKey } = values;
      const workspaceId = getId(workspaceKey);
      return deleteWorkspace(workspaceKey).pipe(
        tap(() => {
          if (resolve) resolve();
        }),
        mergeMap(() =>
          from([
            actions.deleteWorkspace.success({ workspaceId }),
            actions.createEventLog.trigger({
              title: `Successfully deleted workspace ${workspaceKey.name}.`,
              type: LOG_LEVEL.info,
            }),
            actions.switchWorkspace(),
            actions.fetchNodes(),
          ]),
        ),
        startWith(actions.deleteWorkspace.request({ workspaceId })),
        catchErrorWithEventLog((err) => {
          if (reject) reject(err);
          return actions.deleteWorkspace.failure(merge(err, { workspaceId }));
        }),
        takeUntil(action$.pipe(ofType(actions.deleteWorkspace.CANCEL))),
      );
    }),
  );
