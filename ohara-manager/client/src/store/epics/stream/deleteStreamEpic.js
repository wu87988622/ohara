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
  catchError,
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
import { CELL_STATUS, LOG_LEVEL } from 'const';

/* eslint-disable no-unused-expressions */
export const deleteStream$ = (value) => {
  const { params, options = {} } = value;
  const streamId = getId(params);

  const previousStatus =
    options.paperApi?.getCell(params?.id)?.status || CELL_STATUS.stopped;

  options.paperApi?.updateElement(params.id, {
    status: CELL_STATUS.pending,
  });

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
      options.paperApi?.removeElement(params.id);

      return from([
        actions.setSelectedCell.trigger(null),
        actions.deleteStream.success({ streamId }),
      ]);
    }),
    startWith(actions.deleteStream.request({ streamId })),
    catchError((err) => {
      options.paperApi?.updateElement(params.id, {
        status: err?.data?.state?.toLowerCase() ?? previousStatus,
      });

      return from([
        actions.deleteStream.failure(merge(err, { streamId })),
        actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
      ]);
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
