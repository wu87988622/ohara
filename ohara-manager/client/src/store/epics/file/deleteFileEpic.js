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
import { from, defer } from 'rxjs';
import {
  catchError,
  map,
  startWith,
  distinctUntilChanged,
  mergeMap,
} from 'rxjs/operators';

import * as fileApi from 'api/fileApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { LOG_LEVEL } from 'const';

export default action$ =>
  action$.pipe(
    ofType(actions.deleteFile.TRIGGER),
    map(action => action.payload),
    distinctUntilChanged(),
    mergeMap(params => {
      const fileId = getId(params);
      return defer(() => fileApi.remove(params)).pipe(
        map(() => actions.deleteFile.success({ fileId })),
        startWith(actions.deleteFile.request({ fileId })),
        catchError(err =>
          from([
            actions.deleteFile.failure(err),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]),
        ),
      );
    }),
  );
