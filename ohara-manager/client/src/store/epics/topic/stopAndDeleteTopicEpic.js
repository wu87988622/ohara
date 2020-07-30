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
import { from, throwError } from 'rxjs';
import { catchError, map, mergeMap, concatAll, tap } from 'rxjs/operators';

import * as actions from 'store/actions';
import { stopTopic$ } from './stopTopicEpic';
import { deleteTopic$ } from './deleteTopicEpic';
import { LOG_LEVEL, CELL_STATUS } from 'const';

// topic is not really a "component" in UI, i.e, we don't have actions on it
// we should combine "delete + stop" for single deletion request
/* eslint-disable no-unused-expressions */
export default (action$) =>
  action$.pipe(
    ofType(actions.stopAndDeleteTopic.TRIGGER),
    map((action) => action.payload),
    mergeMap((values) => {
      const { params, options = {}, promise } = values;

      const previousStatus =
        options.paperApi?.getCell(params?.id)?.status || CELL_STATUS.stopped;

      options.paperApi?.updateElement(params.id, {
        status: CELL_STATUS.pending,
      });
      return from([
        stopTopic$(params).pipe(
          catchError((err) => {
            options.paperApi?.updateElement(params.id, {
              status: CELL_STATUS.running,
            });
            if (typeof promise?.reject === 'function') {
              promise.reject(err);
            }
            return throwError(err);
          }),
        ),
        deleteTopic$(params).pipe(
          tap((action) => {
            if (action.type === actions.deleteTopic.SUCCESS) {
              if (typeof promise?.resolve === 'function') {
                promise.resolve();
              }
              options.paperApi?.removeElement(params.id);
            }
          }),
          catchError((err) => {
            options.paperApi?.updateElement(params.id, {
              status: err?.data?.state?.toLowerCase() ?? previousStatus,
            });
            if (typeof promise?.reject === 'function') {
              promise.reject(err);
            }

            return throwError(err);
          }),
        ),
      ]).pipe(
        concatAll(),
        catchError((err) => {
          return from([
            actions.stopAndDeleteTopic.failure(err),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]);
        }),
      );
    }),
  );
