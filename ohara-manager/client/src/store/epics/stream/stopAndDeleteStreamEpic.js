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
import { of } from 'rxjs';
import { catchError, map, mergeMap, concatAll } from 'rxjs/operators';

import * as actions from 'store/actions';
import stopStream$ from './stopStreamEpic';
import deleteStream$ from './deleteStreamEpic';

export default action$ =>
  action$.pipe(
    ofType(actions.stopAndDeleteStream.REQUEST),
    map(action => action.payload),
    mergeMap(values =>
      of(stopStream$(values), deleteStream$(values)).pipe(
        concatAll(),
        catchError(res => of(actions.stopAndDeleteStream.failure(res))),
      ),
    ),
  );
