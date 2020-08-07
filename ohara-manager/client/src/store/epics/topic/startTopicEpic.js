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
import { of, defer, iif, throwError, zip } from 'rxjs';
import {
  map,
  startWith,
  retryWhen,
  delay,
  concatMap,
  distinctUntilChanged,
  mergeMap,
} from 'rxjs/operators';

import * as topicApi from 'api/topicApi';
import { State } from 'api/apiInterface/topicInterface';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';
import { catchErrorWithEventLog } from '../utils';

// Note: The caller SHOULD handle the error of this action
export const startTopic$ = (params) => {
  const topicId = getId(params);
  return zip(
    defer(() => topicApi.start(params)),
    defer(() => topicApi.get(params)).pipe(
      map((res) => {
        if (!res.data.state || res.data.state !== State.RUNNING) throw res;
        return res.data;
      }),
    ),
  ).pipe(
    retryWhen((errors) =>
      errors.pipe(
        concatMap((value, index) =>
          iif(
            () => index > 10,
            throwError({
              data: value?.data,
              meta: value?.meta,
              title:
                `Try to start topic: "${params.name}" failed after retry ${index} times. ` +
                `Expected state: RUNNING, Actual state: ${value.data?.state}`,
            }),
            of(value).pipe(delay(2000)),
          ),
        ),
      ),
    ),
    map(([, data]) => normalize(data, schema.topic)),
    map((normalizedData) => merge(normalizedData, { topicId })),
    map((normalizedData) => actions.startTopic.success(normalizedData)),
    startWith(actions.startTopic.request({ topicId })),
  );
};

export default (action$) =>
  action$.pipe(
    ofType(actions.startTopic.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap((values) =>
      startTopic$(values).pipe(
        catchErrorWithEventLog((err) =>
          actions.startTopic.failure(merge(err, { topicId: getId(values) })),
        ),
      ),
    ),
  );
