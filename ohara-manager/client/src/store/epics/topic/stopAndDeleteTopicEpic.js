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
import { from, of } from 'rxjs';
import { catchError, map, mergeMap, concatAll } from 'rxjs/operators';

import * as actions from 'store/actions';
import { LOG_LEVEL } from 'const';
import { stopTopic$ } from './stopTopicEpic';
import { deleteTopic$ } from './deleteTopicEpic';

// topic is not really a "component" in UI, i.e, we don't have actions on it
// we should combine "delete + stop" for single deletion request
export default action$ =>
  action$.pipe(
    ofType(actions.stopAndDeleteTopic.REQUEST),
    map(action => action.payload),
    mergeMap(values =>
      of(stopTopic$(values), deleteTopic$(values)).pipe(
        concatAll(),
        catchError(res =>
          from([
            actions.stopAndDeleteTopic.failure(res),
            actions.createEventLog.trigger({ ...res, type: LOG_LEVEL.error }),
          ]),
        ),
      ),
    ),
  );
