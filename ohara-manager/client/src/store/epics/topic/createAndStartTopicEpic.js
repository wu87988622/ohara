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

import { get } from 'lodash';
import { ofType } from 'redux-observable';
import { of, from } from 'rxjs';
import { catchError, map, concatAll, mergeMap, tap } from 'rxjs/operators';

import * as actions from 'store/actions';
import { createTopic$ } from './createTopicEpic';
import { startTopic$ } from './startTopicEpic';
import { LOG_LEVEL, CELL_STATUS } from 'const';

// topic is not really a "component" in UI, i.e, we don't have actions on it
// we should combine "create + start" for single creation request
export default action$ =>
  action$.pipe(
    ofType(actions.createAndStartTopic.TRIGGER),
    map(action => action.payload),
    mergeMap(values => {
      const { params, options } = values;
      const paperApi = get(options, 'paperApi');
      if (paperApi) {
        paperApi.updateElement(params.id, {
          status: CELL_STATUS.pending,
        });
      }
      return of(createTopic$(params), startTopic$(params)).pipe(
        concatAll(),
        tap(() => {
          if (paperApi) {
            paperApi.updateElement(params.id, {
              status: CELL_STATUS.running,
            });
          }
          if (options?.onSuccess) options.onSuccess();
        }),
        catchError(res => {
          if (paperApi) {
            paperApi.updateElement(params.id, {
              status: CELL_STATUS.failed,
            });
          }
          if (options?.onError) options.onError(res);
          return from([
            actions.createAndStartTopic.failure(res),
            actions.createEventLog.trigger({ ...res, type: LOG_LEVEL.error }),
          ]);
        }),
      );
    }),
  );
