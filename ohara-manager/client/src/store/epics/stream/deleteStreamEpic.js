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

import * as streamApi from 'api/streamApi';
import * as actions from 'store/actions';

export default action$ =>
  action$.pipe(
    ofType(actions.deleteStream.TRIGGER),
    map(action => action.payload),
    switchMap(({ params, options }) => {
      return from(streamApi.remove(params)).pipe(
        map(() => {
          handleSuccess(options);
          return actions.deleteStream.success(params);
        }),
        startWith(actions.deleteStream.request()),
        catchError(res => of(actions.deleteStream.failure(res))),
      );
    }),
  );

function handleSuccess(options) {
  const { id, paperApi } = options;
  if (paperApi) {
    paperApi.removeElement(id);
  }
}
