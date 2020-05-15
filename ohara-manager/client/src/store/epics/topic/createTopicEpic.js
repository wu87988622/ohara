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
import { defer, from, of } from 'rxjs';
import {
  catchError,
  map,
  mergeMap,
  distinctUntilChanged,
  startWith,
} from 'rxjs/operators';

import * as topicApi from 'api/topicApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import * as schema from 'store/schema';
import { LOG_LEVEL } from 'const';

// Note: The caller SHOULD handle the error of this action
export const createTopic$ = params => {
  const topicId = getId(params);
  return defer(() => topicApi.create(params)).pipe(
    mergeMap(res => {
      const normalizedData = normalize(res.data, schema.topic);
      return of(
        actions.createTopic.success(merge(normalizedData, { topicId })),
      );
    }),
    startWith(actions.createTopic.request({ topicId })),
  );
};

export default action$ =>
  action$.pipe(
    ofType(actions.createTopic.TRIGGER),
    map(action => action.payload),
    distinctUntilChanged(),
    mergeMap(values =>
      createTopic$(values).pipe(
        catchError(err =>
          from([
            actions.createTopic.failure(merge(err, { topicId: getId(values) })),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]),
        ),
      ),
    ),
  );
