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

import { ofType } from 'redux-observable';
import { defer, from } from 'rxjs';
import { catchError, map, switchMap, mergeMap } from 'rxjs/operators';

import * as topicApi from 'api/topicApi';
import * as actions from 'store/actions';
import { LOG_LEVEL } from 'const';
import { getId } from 'utils/object';

const handleSuccess = values => {
  const { id, paperApi } = values;
  if (paperApi) {
    paperApi.removeElement(id);
  }
};

export const deleteTopic$ = values =>
  defer(() => topicApi.remove(values)).pipe(
    mergeMap(res => {
      handleSuccess(values);
      const id = getId(values);
      return from([
        actions.deleteTopic.success(id),
        actions.createEventLog.trigger({ ...res, type: LOG_LEVEL.info }),
      ]);
    }),
    catchError(res =>
      from([
        actions.deleteTopic.failure(res),
        actions.createEventLog.trigger({ ...res, type: LOG_LEVEL.error }),
      ]),
    ),
  );

export default action$ =>
  action$.pipe(
    ofType(actions.deleteTopic.REQUEST),
    map(action => action.payload),
    switchMap(values =>
      deleteTopic$(values).pipe(
        catchError(res =>
          from([
            actions.deleteTopic.failure(res),
            actions.createEventLog.trigger({ ...res, type: LOG_LEVEL.error }),
          ]),
        ),
      ),
    ),
  );
