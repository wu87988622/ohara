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
import { normalize } from 'normalizr';
import { of, zip, defer } from 'rxjs';
import {
  switchMap,
  map,
  startWith,
  catchError,
  retryWhen,
  delay,
  take,
} from 'rxjs/operators';

import * as streamApi from 'api/streamApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getCellState } from 'components/Pipeline/PipelineApiHelper/apiHelperUtils';
import { CELL_STATUS } from 'const';

export default action$ => {
  return action$.pipe(
    ofType(actions.startStream.TRIGGER),
    map(action => action.payload),
    switchMap(({ params, options }) => {
      const { id, paperApi } = options;
      if (paperApi) {
        paperApi.updateElement(id, {
          status: CELL_STATUS.pending,
        });
      }

      return zip(
        defer(() => streamApi.start(params)),
        defer(() => streamApi.get(params)).pipe(
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
          handleSuccess(options, res);
          return normalize(res.data, schema.stream);
        }),
        map(normalizedData => actions.startStream.success(normalizedData)),
        startWith(actions.startStream.request()),
        catchError(err => {
          handleError(options);
          return of(actions.startStream.failure(err));
        }),
      );
    }),
  );
};

function handleSuccess(options, res) {
  const { id, paperApi } = options;

  if (paperApi) {
    paperApi.updateElement(id, {
      status: getCellState(res),
    });
  }
}

function handleError(options) {
  const { id, paperApi } = options;

  if (paperApi) {
    paperApi.updateElement(id, {
      status: CELL_STATUS.stopped,
    });
  }
}
