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
import { defer, from, zip, throwError, iif, of } from 'rxjs';
import {
  catchError,
  map,
  retryWhen,
  delay,
  distinctUntilChanged,
  mergeMap,
  startWith,
  concatMap,
} from 'rxjs/operators';

import * as topicApi from 'api/topicApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';
import { LOG_LEVEL } from 'const';

export const stopTopic$ = params => {
  const topicId = getId(params);
  return zip(
    defer(() => topicApi.stop(params)),
    defer(() => topicApi.get(params)).pipe(
      map(res => {
        if (res.data.state) throw res;
        else return res.data;
      }),
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
    map(([, data]) => normalize(data, schema.topic)),
    map(normalizedData => merge(normalizedData, { topicId })),
    map(normalizedData => actions.stopTopic.success(normalizedData)),
    startWith(actions.stopTopic.request({ topicId })),
    catchError(error =>
      // Let the caller decides this Action should be terminated or trigger failure reducer
      throwError(error),
    ),
  );
};

export default action$ =>
  action$.pipe(
    ofType(actions.stopTopic.TRIGGER),
    map(action => action.payload),
    distinctUntilChanged(),
    mergeMap(values =>
      stopTopic$(values).pipe(
        catchError(error =>
          from([
            actions.stopTopic.failure(error),
            actions.createEventLog.trigger({
              title: error,
              type: LOG_LEVEL.error,
            }),
          ]),
        ),
      ),
    ),
  );
