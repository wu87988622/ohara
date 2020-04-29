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
import { defer, from } from 'rxjs';
import {
  catchError,
  map,
  startWith,
  distinctUntilChanged,
  mergeMap,
} from 'rxjs/operators';

import { CELL_STATUS, LOG_LEVEL } from 'const';
import * as streamApi from 'api/streamApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';

const createStream$ = value => {
  const { values, options } = value;
  const streamId = getId(values);
  return defer(() => streamApi.create(values)).pipe(
    map(res => res.data),
    map(data => normalize(data, schema.stream)),
    map(normalizedData => {
      options.paperApi.updateElement(values.id, {
        status: CELL_STATUS.stopped,
      });
      return merge(normalizedData, { streamId });
    }),
    map(normalizedData => actions.createStream.success(normalizedData)),
    startWith(actions.createStream.request({ streamId })),
    catchError(error => {
      options.paperApi.removeElement(values.id);
      return from([
        actions.createStream.failure(merge(error, { streamId })),
        actions.createEventLog.trigger({ ...error, type: LOG_LEVEL.error }),
      ]);
    }),
  );
};

export default action$ =>
  action$.pipe(
    ofType(actions.createStream.TRIGGER),
    map(action => action.payload),
    distinctUntilChanged(),
    mergeMap(value => createStream$(value)),
  );
