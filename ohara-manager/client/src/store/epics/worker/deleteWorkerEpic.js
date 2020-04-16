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
import { defer, of } from 'rxjs';
import {
  catchError,
  map,
  startWith,
  mergeMap,
  distinctUntilChanged,
} from 'rxjs/operators';

import * as workerApi from 'api/workerApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';

const deleteWorker$ = params => {
  const workerId = getId(params);
  return defer(() => workerApi.remove(params)).pipe(
    map(() => actions.deleteWorker.success({ workerId })),
    startWith(actions.deleteWorker.request({ workerId })),
    catchError(error =>
      of(actions.deleteWorker.failure(merge(error, { workerId }))),
    ),
  );
};

export default action$ =>
  action$.pipe(
    ofType(actions.deleteWorker.TRIGGER),
    map(action => action.payload),
    distinctUntilChanged(),
    mergeMap(params => deleteWorker$(params)),
  );
