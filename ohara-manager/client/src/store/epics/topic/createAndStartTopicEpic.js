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
import { of, from, throwError } from 'rxjs';
import { catchError, map, concatAll, mergeMap, tap } from 'rxjs/operators';

import * as actions from 'store/actions';
import { createTopic$ } from './createTopicEpic';
import { startTopic$ } from './startTopicEpic';
import { LOG_LEVEL, CELL_STATUS } from 'const';

// topic is not really a "component" in UI, i.e, we don't have actions on it
// we should combine "create + start" for single creation request
/* eslint-disable no-unused-expressions */
export default (action$) =>
  action$.pipe(
    ofType(actions.createAndStartTopic.TRIGGER),
    map((action) => action.payload),
    mergeMap((values) => {
      const { params, options = {}, promise } = values;
      options.paperApi?.updateElement(params.id, {
        status: CELL_STATUS.pending,
      });
      return of(
        createTopic$(params).pipe(
          tap((action) => {
            if (action.type === actions.createTopic.SUCCESS) {
              if (typeof promise?.resolve === 'function') {
                promise.resolve();
              }
            }
          }),
          catchError((err) => {
            options.paperApi?.removeElement(params.id);
            if (typeof promise?.reject === 'function') {
              promise.reject(err);
            }

            return throwError(err);
          }),
        ),
        startTopic$(params).pipe(
          tap((action) => {
            if (action.type === actions.startTopic.SUCCESS) {
              options.paperApi?.updateElement(params.id, {
                status: CELL_STATUS.running,
              });
            }
          }),
          catchError((err) => {
            options.paperApi?.updateElement(params.id, {
              status: err?.data?.state?.toLowerCase() ?? CELL_STATUS.stopped,
            });
            if (typeof promise?.reject === 'function') {
              promise.reject(err);
            }

            return throwError(err);
          }),
        ),
      ).pipe(
        concatAll(),
        catchError((err) => {
          return from([
            actions.createAndStartTopic.failure(err),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]);
        }),
      );
    }),
  );
