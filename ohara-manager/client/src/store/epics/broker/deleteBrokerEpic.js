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
import { ofType } from 'redux-observable';
import { from } from 'rxjs';
import {
  catchError,
  map,
  startWith,
  mergeMap,
  distinctUntilChanged,
} from 'rxjs/operators';

import * as brokerApi from './apiEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { LOG_LEVEL } from 'const';

// Note: The caller SHOULD handle the error of this action
export const deleteBroker$ = params => {
  const brokerId = getId(params);
  return brokerApi.remove$(params).pipe(
    map(() => actions.deleteBroker.success({ brokerId })),
    startWith(actions.deleteBroker.request({ brokerId })),
  );
};

export default action$ =>
  action$.pipe(
    ofType(actions.deleteBroker.TRIGGER),
    map(action => action.payload),
    distinctUntilChanged(),
    mergeMap(params =>
      deleteBroker$(params).pipe(
        catchError(err =>
          from([
            actions.deleteBroker.failure(
              merge(err, { brokerId: getId(params) }),
            ),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]),
        ),
      ),
    ),
  );
