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

import _ from 'lodash';
import { ofType } from 'redux-observable';
import { defer, zip } from 'rxjs';
import { switchMap, map, mergeMap, defaultIfEmpty } from 'rxjs/operators';
import localForage from 'localforage';

import * as actions from 'store/actions';
import { catchErrorWithEventLog } from '../utils';

localForage.config({
  name: 'ohara',
  storeName: 'event_logs',
});

const getLogFromLocalForge$ = () =>
  defer(() => localForage.keys()).pipe(
    mergeMap((keys) =>
      zip(..._.map(keys, (key) => defer(() => localForage.getItem(key)))),
    ),
    map((values) =>
      values.map((value) => ({
        key: _.get(value, 'key'),
        type: _.get(value, 'type'),
        title: _.get(value, 'title'),
        createAt: _.get(value, 'createAt'),
        payload: _.get(value, 'payload'),
      })),
    ),
  );

export default (action$) =>
  action$.pipe(
    ofType(actions.fetchEventLogs.TRIGGER),
    switchMap(() =>
      getLogFromLocalForge$().pipe(
        defaultIfEmpty(),
        map((data) => actions.fetchEventLogs.success(data)),
        catchErrorWithEventLog((err) => actions.fetchEventLogs.failure(err)),
      ),
    ),
  );
