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
import { merge } from 'lodash';
import { ofType } from 'redux-observable';
import {
  map,
  switchMap,
  startWith,
  throttleTime,
  takeUntil,
  endWith,
} from 'rxjs/operators';

import { fetchWorker } from 'observables';

import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';
import { catchErrorWithEventLog } from '../utils';

const fetchWorker$ = (params) => {
  const workerId = getId(params);
  return fetchWorker(params).pipe(
    map((data) => merge(normalize(data, schema.worker), { workerId })),
    map((normalizedData) => actions.fetchWorker.success(normalizedData)),
    startWith(actions.fetchWorker.request({ workerId })),
    endWith(actions.inspectWorker.trigger(params)),
    catchErrorWithEventLog((err) =>
      actions.fetchWorker.failure(merge(err, { workerId })),
    ),
  );
};

export default (action$) =>
  action$.pipe(
    ofType(actions.fetchWorker.TRIGGER),
    map((action) => action.payload),
    throttleTime(1000),
    switchMap((params) =>
      fetchWorker$(params).pipe(
        // Stop fetching worker info once the delete workspace action is triggered
        takeUntil(action$.pipe(ofType(actions.deleteWorkspace.TRIGGER))),
      ),
    ),
  );
