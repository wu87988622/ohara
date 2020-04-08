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

import { normalize } from 'normalizr';
import { ofType } from 'redux-observable';
import { of, defer, iif, throwError } from 'rxjs';
import {
  catchError,
  map,
  startWith,
  switchMap,
  retryWhen,
  delay,
  concatMap,
  concatAll,
} from 'rxjs/operators';

import * as connectorApi from 'api/connectorApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { CELL_STATUS } from 'const';

const checkState$ = (values, options) =>
  defer(() =>
    connectorApi.get({ name: values.name, group: values.group }),
  ).pipe(
    map(res => normalize(res.data, schema.connector)),
    map(res => iif(() => !res.state === CELL_STATUS.running, throwError, res)),
    retryWhen(error =>
      error.pipe(
        concatMap((e, i) =>
          iif(() => i > 4, throwError(e), of(e).pipe(delay(2000))),
        ),
      ),
    ),
    map(res => {
      options.paperApi.updateElement(values.id, {
        status: CELL_STATUS.running,
      });
      return actions.startConnector.success(res);
    }),
    startWith(actions.fetchConnector.request()),
  );

export default action$ => {
  return action$.pipe(
    ofType(actions.startConnector.TRIGGER),
    map(action => action.payload),
    switchMap(({ params, options }) => {
      options.paperApi.updateElement(params.id, {
        status: CELL_STATUS.pending,
      });
      return of(
        defer(() => connectorApi.start(params)).pipe(
          map(() => actions.startConnector.request()),
        ),
        checkState$(params, options),
      ).pipe(
        concatAll(),
        catchError(res => {
          options.paperApi.updateElement(params.id, {
            status: CELL_STATUS.failed,
          });
          return of(actions.startConnector.failure(res));
        }),
      );
    }),
  );
};
