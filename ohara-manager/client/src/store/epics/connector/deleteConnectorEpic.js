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
import { of, defer, iif, throwError, zip, from } from 'rxjs';
import {
  map,
  startWith,
  retryWhen,
  delay,
  concatMap,
  distinctUntilChanged,
  mergeMap,
} from 'rxjs/operators';

import { CELL_STATUS } from 'const';
import * as connectorApi from 'api/connectorApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { catchErrorWithEventLog } from '../utils';

export const deleteConnector$ = (values) => {
  const { params, options = {} } = values;
  const { paperApi } = options;
  const connectorId = getId(params);

  if (paperApi) {
    paperApi.updateElement(params.id, {
      status: CELL_STATUS.pending,
    });
  }

  return zip(
    defer(() => connectorApi.remove(params)),
    defer(() => connectorApi.getAll({ group: params.group })).pipe(
      map((res) => {
        if (res.data.find((connector) => connector.name === params.name))
          throw res;
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
              title: `Try to remove connector: "${params.name}" failed after retry ${index} times.`,
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
        actions.deleteConnector.success({ connectorId }),
      ]);
    }),
    startWith(actions.deleteConnector.request({ connectorId })),
    catchErrorWithEventLog((err) => {
      if (paperApi) {
        paperApi.updateElement(params.id, {
          status: CELL_STATUS.failed,
        });
      }

      return actions.deleteConnector.failure(merge(err, { connectorId }));
    }),
  );
};

export default (action$) => {
  return action$.pipe(
    ofType(actions.deleteConnector.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap((values) => deleteConnector$(values)),
  );
};
