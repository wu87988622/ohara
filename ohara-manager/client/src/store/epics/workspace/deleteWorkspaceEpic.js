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
import { from, of } from 'rxjs';
import { catchError, map, switchMap, startWith } from 'rxjs/operators';

import * as workspaceApi from 'api/workspaceApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';

export default action$ =>
  action$.pipe(
    ofType(actions.deleteWorkspace.TRIGGER),
    map(action => action.payload),
    switchMap(params =>
      from(workspaceApi.remove(params)).pipe(
        map(params => getId(params)),
        map(id => actions.deleteWorkspace.success(id)),
        startWith(actions.deleteWorkspace.request()),
        catchError(res => of(actions.deleteWorkspace.failure(res))),
      ),
    ),
  );
