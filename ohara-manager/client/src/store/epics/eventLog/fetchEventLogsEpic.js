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

import { get } from 'lodash';
import { ofType } from 'redux-observable';
import { from, of } from 'rxjs';
import { switchMap, map, catchError } from 'rxjs/operators';
import localForage from 'localforage';

import * as actions from 'store/actions';

localForage.config({
  name: 'ohara',
  storeName: 'event_logs',
});

const getLogFromLocalForge$ = () =>
  from(
    localForage.keys().then(keys => {
      const data = [];
      for (let key of keys) {
        data.push(
          localForage.getItem(key).then(value => ({
            key,
            type: get(value, 'type'),
            title: get(value, 'title'),
            createAt: get(value, 'createAt'),
            payload: get(value, 'payload'),
          })),
        );
      }
      return Promise.all(data);
    }),
  );

export default action$ =>
  action$.pipe(
    ofType(actions.fetchEventLogs.TRIGGER),
    switchMap(() =>
      getLogFromLocalForge$().pipe(
        map(data => actions.fetchEventLogs.success(data)),
        catchError(res => of(actions.fetchEventLogs.failure(res))),
      ),
    ),
  );
