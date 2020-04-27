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
import { LOG_LEVEL } from 'const';

export const deleteTopic$ = params => {
  const topicId = getId(params);
  return defer(() => topicApi.remove(params)).pipe(
    mergeMap(res =>
      from([
        actions.deleteTopic.success({ topicId }),
        actions.createEventLog.trigger({ ...res, type: LOG_LEVEL.info }),
      ]),
    ),
    startWith(actions.deleteTopic.request({ topicId })),
    catchError(error =>
      from([
        actions.deleteTopic.failure(error),
        actions.createEventLog.trigger({ ...error, type: LOG_LEVEL.error }),
      ]),
    ),
  );
};

export default action$ =>
  action$.pipe(
    ofType(actions.deleteTopic.TRIGGER),
    map(action => action.payload),
    distinctUntilChanged(),
    mergeMap(values => deleteTopic$(values)),
  );
