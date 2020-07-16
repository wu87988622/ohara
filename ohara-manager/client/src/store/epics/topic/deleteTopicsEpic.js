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
  distinctUntilChanged,
  map,
  mergeMap,
  startWith,
  tap,
  takeUntil,
} from 'rxjs/operators';

import { LOG_LEVEL } from 'const';
import { fetchAndDeleteTopics } from 'observables';
import { getId } from 'utils/object';
import * as actions from 'store/actions';

export default (action$) =>
  action$.pipe(
    ofType(actions.deleteTopics.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap(({ values, resolve, reject }) => {
      const { workspaceKey } = values;
      return fetchAndDeleteTopics(workspaceKey).pipe(
        tap((topics) => {
          if (resolve) resolve(topics);
          return topics;
        }),
        map((topics) => actions.deleteTopics.success(topics.map(getId))),
        startWith(actions.deleteTopics.request()),
        catchError((err) => {
          if (reject) reject(err);
          return from([
            actions.deleteTopics.failure(merge(err)),
            actions.createEventLog.trigger({
              ...err,
              type: LOG_LEVEL.error,
            }),
          ]);
        }),
        takeUntil(action$.pipe(ofType(actions.deleteTopics.CANCEL))),
      );
    }),
  );
