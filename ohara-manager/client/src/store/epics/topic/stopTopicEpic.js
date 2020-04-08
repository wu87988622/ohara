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
import { defer, from, zip, throwError } from 'rxjs';
import {
  catchError,
  map,
  switchMap,
  retryWhen,
  delay,
  take,
} from 'rxjs/operators';

import * as topicApi from 'api/topicApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { LOG_LEVEL } from 'const';

export const stopTopic$ = params =>
  zip(
    defer(() => topicApi.stop(params)),
    defer(() => topicApi.get(params)).pipe(
      map(res => {
        if (res.data.state) {
          throw res;
        }
        return res;
      }),
      retryWhen(error => error.pipe(delay(1000 * 2), take(5))),
    ),
  ).pipe(
    map(([, res]) => normalize(res.data, schema.topic)),
    map(normalizedData => actions.stopTopic.success(normalizedData)),
    catchError(res =>
      // Let the caller decides this Action should be terminated or trigger failure reducer
      throwError(res),
    ),
  );

export default action$ =>
  action$.pipe(
    ofType(actions.stopTopic.REQUEST),
    map(action => action.payload),
    switchMap(values =>
      stopTopic$(values).pipe(
        catchError(res =>
          from([
            actions.stopTopic.failure(res),
            actions.createEventLog.trigger({ ...res, type: LOG_LEVEL.error }),
          ]),
        ),
      ),
    ),
  );
