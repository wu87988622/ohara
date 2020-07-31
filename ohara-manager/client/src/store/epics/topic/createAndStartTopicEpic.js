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
import { of, from, throwError } from 'rxjs';
import {
  catchError,
  concatAll,
  map,
  mergeMap,
  startWith,
  tap,
} from 'rxjs/operators';

import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';
import { createTopic, startTopic } from 'observables';
import { CELL_STATUS, LOG_LEVEL } from 'const';

// topic is not really a "component" in UI, i.e, we don't have actions on it
// we should combine "create + start" for single creation request
export default (action$) =>
  action$.pipe(
    ofType(actions.createAndStartTopic.TRIGGER),
    map((action) => action.payload),
    mergeMap(({ values = {}, options = {}, resolve, reject }) => {
      const topicId = getId(values);
      const previousStatus =
        options.paperApi?.getCell(values?.id)?.status || CELL_STATUS.stopped;
      const updateStatus = (status) =>
        options.paperApi?.updateElement(values.id, {
          status,
        });

      updateStatus(CELL_STATUS.pending);
      return of(
        createTopic(values).pipe(
          tap((data) => typeof resolve === 'function' && resolve(data)),
          map((data) => normalize(data, schema.topic)),
          map((normalizedData) => merge(normalizedData, { topicId })),
          map((normalizedData) => actions.createTopic.success(normalizedData)),
          startWith(actions.createTopic.request({ topicId })),
          catchError((err) => {
            if (options.paperApi?.removeElement)
              options.paperApi.removeElement(values.id);
            return throwError(err);
          }),
        ),
        startTopic(values).pipe(
          tap(() => updateStatus(CELL_STATUS.running)),
          map((data) => normalize(data, schema.topic)),
          map((normalizedData) => merge(normalizedData, { topicId })),
          map((normalizedData) => actions.startTopic.success(normalizedData)),
          startWith(actions.startTopic.request({ topicId })),
          catchError((err) => {
            updateStatus(err?.data?.state?.toLowerCase() ?? previousStatus);
            return throwError(err);
          }),
        ),
      ).pipe(
        concatAll(),
        catchError((err) => {
          if (typeof reject === 'function') reject(err);
          return from([
            actions.createAndStartTopic.failure(merge(err, { topicId })),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]);
        }),
      );
    }),
  );
