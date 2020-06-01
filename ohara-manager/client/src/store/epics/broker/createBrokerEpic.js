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
import { from } from 'rxjs';
import {
  catchError,
  distinctUntilChanged,
  map,
  startWith,
  mergeMap,
  switchMap,
} from 'rxjs/operators';

import * as brokerApi from './apiEpic';
import * as actions from 'store/actions';
import { LOG_LEVEL, GROUP } from 'const';

export const createBroker$ = values =>
  brokerApi.create$(values).pipe(
    map(res => res.data),
    switchMap(data => {
      return from([
        actions.updateWorkspace.trigger({
          broker: data,
          // the workspace name of current object should be as same as the broker name
          name: values.name,
          group: GROUP.WORKSPACE,
        }),
        actions.createBroker.success(data),
      ]);
    }),
    startWith(actions.createBroker.request(values)),
    catchError(err =>
      from([
        actions.createBroker.failure(err),
        actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
      ]),
    ),
  );

export default action$ =>
  action$.pipe(
    ofType(actions.createBroker.TRIGGER),
    map(action => action.payload),
    distinctUntilChanged(),
    mergeMap(values => createBroker$(values)),
  );
