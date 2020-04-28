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
import { of, defer } from 'rxjs';
import { catchError, map, startWith, switchMap } from 'rxjs/operators';

import * as connectorApi from 'api/connectorApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';

export default action$ =>
  action$.pipe(
    ofType(actions.fetchConnectors.TRIGGER),
    map(action => action.payload),
    switchMap(values =>
      defer(() => connectorApi.getAll(values)).pipe(
        map(res => normalize(res.data, [schema.connector])),
        map(normalizedData => actions.fetchConnectors.success(normalizedData)),
        startWith(actions.fetchConnectors.request()),
        catchError(error => of(actions.fetchConnectors.failure(error))),
      ),
    ),
  );
