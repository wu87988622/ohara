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
import { of, zip, defer, iif, throwError, from } from 'rxjs';
import {
  map,
  startWith,
  distinctUntilChanged,
  mergeMap,
  retryWhen,
  concatMap,
  delay,
} from 'rxjs/operators';

import * as streamApi from 'api/streamApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { CELL_STATUS } from 'const';
import { catchErrorWithEventLog } from '../utils';

export const deleteStream$ = (value) => {
  const { params, options = {} } = value;
  const { paperApi } = options;
  const streamId = getId(params);

  if (paperApi) {
    paperApi.updateElement(params.id, {
      status: CELL_STATUS.pending,
    });
  }

  return zip(
    defer(() => streamApi.remove(params)),
    defer(() => streamApi.getAll({ group: params.group })).pipe(
      map((res) => {
        if (res.data.find((stream) => stream.name === params.name)) throw res;
        else return res.data;
      }),
    ),
  ).pipe(
    retryWhen((errors) =>
      errors.pipe(
        concatMap((value, index) =>
          iif(
            () => index > 4,
            throwError({
              data: value?.data,
              meta: value?.meta,
              title: `Try to remove stream: "${params.name}" failed after retry ${index} times.`,
            }),
            of(value).pipe(delay(2000)),
          ),
        ),
      ),
    ),
    mergeMap(() => {
      if (paperApi) {
        paperApi.removeElement(params.id);
      }

      return from([
        actions.setSelectedCell.trigger(null),
        actions.deleteStream.success({ streamId }),
      ]);
    }),
    startWith(actions.deleteStream.request({ streamId })),
    catchErrorWithEventLog((error) => {
      if (paperApi) {
        paperApi.updateElement(params.id, {
          status: CELL_STATUS.failed,
        });
      }

      return actions.deleteStream.failure(merge(error, { streamId }));
    }),
  );
};

export default (action$) => {
  return action$.pipe(
    ofType(actions.deleteStream.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap((value) => deleteStream$(value)),
  );
};
