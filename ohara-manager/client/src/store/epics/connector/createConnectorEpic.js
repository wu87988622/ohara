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
import { from, of } from 'rxjs';
import { catchError, map, startWith, switchMap } from 'rxjs/operators';

import * as connectorApi from 'api/connectorApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { CELL_STATUS } from 'const';

export default action$ => {
  return action$.pipe(
    ofType(actions.createConnector.TRIGGER),
    map(action => action.payload),
    switchMap(({ params, options }) =>
      from(connectorApi.create(params)).pipe(
        map(res => normalize(res.data, schema.connector)),
        map(entities => {
          options.paperApi.updateElement(params.id, {
            status: CELL_STATUS.stopped,
          });
          return actions.createConnector.success(entities);
        }),
        startWith(actions.createConnector.request()),
        catchError(err => {
          options.paperApi.removeElement(params.id);
          return of(actions.createConnector.failure(err));
        }),
      ),
    ),
  );
};
