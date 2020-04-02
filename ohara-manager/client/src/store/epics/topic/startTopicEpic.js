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
import { defer, from, zip } from 'rxjs';
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
import { getCellState } from 'components/Pipeline/PipelineApiHelper/apiHelperUtils';
import { LOG_LEVEL } from 'hooks';

const handleSuccess = (values, res) => {
  const { id, paperApi } = values;
  if (paperApi) {
    const state = getCellState(res);
    paperApi.updateElement(id, {
      status: state,
    });
  }
};

const handleFail = values => {
  const { id, paperApi } = values;
  if (paperApi) {
    paperApi.removeElement(id);
  }
};

export const startTopic$ = values =>
  zip(
    defer(() => topicApi.start(values)),
    defer(() => topicApi.get(values)).pipe(
      map(res => {
        if (!res.data.state || res.data.state !== 'RUNNING') {
          throw res;
        }
        return res;
      }),
      retryWhen(error => error.pipe(delay(1000 * 2), take(5))),
    ),
  ).pipe(
    map(([, res]) => {
      handleSuccess(values, res);
      return normalize(res.data, schema.topic);
    }),
    map(entities => actions.startTopic.success(entities)),
    catchError(res => {
      handleFail(values);
      return from([
        actions.startTopic.failure(res),
        actions.createEventLog.trigger({ ...res, type: LOG_LEVEL.error }),
      ]);
    }),
  );

export default action$ =>
  action$.pipe(
    ofType(actions.startTopic.REQUEST),
    map(action => action.payload),
    switchMap(values => startTopic$(values)),
    catchError(res =>
      from([
        actions.startTopic.failure(res),
        actions.createEventLog.trigger({ ...res, type: LOG_LEVEL.error }),
      ]),
    ),
  );
