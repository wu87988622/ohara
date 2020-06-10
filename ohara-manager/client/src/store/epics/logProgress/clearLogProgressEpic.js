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
import { concatMap, map, catchError } from 'rxjs/operators';

import * as actions from 'store/actions';

export default (action$) =>
  action$.pipe(
    ofType(actions.clearLogProgress.TRIGGER),
    map((action) => action.payload),
    concatMap((entity) => from(actions.clearLogProgress.success(entity))),
    catchError((res) => of(actions.clearLogProgress.failure(res))),
  );
