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
import { defer, from, throwError } from 'rxjs';
import { catchError, map, switchMap, mergeMap } from 'rxjs/operators';

import * as topicApi from 'api/topicApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { CELL_STATUS } from 'const';
import { LOG_LEVEL } from 'const';

const handleSuccess = values => {
  const { id, paperApi } = values;
  if (paperApi) {
    paperApi.updateElement(id, {
      status: CELL_STATUS.pending,
    });
  }
};

const handleFail = values => {
  const { id, paperApi } = values;
  if (paperApi) {
    paperApi.removeElement(id);
  }
};

export const createTopic$ = values =>
  defer(() => topicApi.create(values)).pipe(
    mergeMap(res => {
      const normalizedData = normalize(res.data, schema.topic);
      handleSuccess(values);
      return from([
        actions.createTopic.success(normalizedData),
        actions.createEventLog.trigger({ ...res, type: LOG_LEVEL.info }),
      ]);
    }),
    catchError(res => {
      handleFail(values);
      // Let the caller decides this Action should be terminated or trigger failure reducer
      return throwError(res);
    }),
  );

export default action$ =>
  action$.pipe(
    ofType(actions.createTopic.REQUEST),
    map(action => action.payload),
    switchMap(values => createTopic$(values)),
    catchError(res =>
      from([
        actions.createTopic.failure(res),
        actions.createEventLog.trigger({ ...res, type: LOG_LEVEL.error }),
      ]),
    ),
  );
