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
import { of, defer } from 'rxjs';
import { catchError, map, startWith, mergeMap } from 'rxjs/operators';

import * as workspaceApi from 'api/workspaceApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';

const updateWorkspace$ = values => {
  const workspaceId = getId(values);
  return defer(() => workspaceApi.update(values)).pipe(
    map(res => res.data),
    map(data => normalize(data, schema.workspace)),
    map(normalizedData => merge(normalizedData, { workspaceId })),
    map(normalizedData => actions.updateWorkspace.success(normalizedData)),
    startWith(actions.updateWorkspace.request({ workspaceId })),
    catchError(error =>
      of(actions.updateWorkspace.failure(merge(error, { workspaceId }))),
    ),
  );
};

export default action$ =>
  action$.pipe(
    ofType(actions.updateWorkspace.TRIGGER),
    map(action => action.payload),
    mergeMap(values => updateWorkspace$(values)),
  );
