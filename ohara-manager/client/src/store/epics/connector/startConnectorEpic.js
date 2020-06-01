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
import { of, defer, iif, throwError, zip, from } from 'rxjs';
import {
  catchError,
  map,
  startWith,
  retryWhen,
  delay,
  concatMap,
  distinctUntilChanged,
  mergeMap,
} from 'rxjs/operators';

import * as connectorApi from 'api/connectorApi';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';
import { CELL_STATUS, LOG_LEVEL } from 'const';

const startConnector$ = values => {
  const { params, options } = values;
  const { paperApi } = options;
  const connectorId = getId(params);
  paperApi.updateElement(params.id, {
    status: CELL_STATUS.pending,
  });
  return zip(
    defer(() => connectorApi.start(params)),
    defer(() => connectorApi.get(params)).pipe(
      map(res => {
        if (!res.data.state || res.data.state !== SERVICE_STATE.RUNNING)
          throw res;
        else return res.data;
      }),
      retryWhen(errors =>
        errors.pipe(
          concatMap((value, index) =>
            iif(
              () => index > 4,
              throwError({
                data: value?.data,
                meta: value?.meta,
                title:
                  `Try to start connector: "${params.name}" failed after retry ${index} times. ` +
                  `Expected state: ${SERVICE_STATE.RUNNING}, Actual state: ${value.data.state}`,
              }),
              of(value).pipe(delay(2000)),
            ),
          ),
        ),
      ),
    ),
  ).pipe(
    map(([, data]) => normalize(data, schema.connector)),
    map(normalizedData => merge(normalizedData, { connectorId })),
    map(normalizedData => {
      paperApi.updateElement(params.id, {
        status: CELL_STATUS.running,
      });
      return actions.startConnector.success(normalizedData);
    }),
    startWith(actions.startConnector.request({ connectorId })),
    catchError(err => {
      options.paperApi.updateElement(params.id, {
        status: CELL_STATUS.stopped,
      });
      return from([
        actions.startConnector.failure(merge(err, { connectorId })),
        actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
      ]);
    }),
  );
};

export default action$ =>
  action$.pipe(
    ofType(actions.startConnector.TRIGGER),
    map(action => action.payload),
    distinctUntilChanged(),
    mergeMap(values => startConnector$(values)),
  );
