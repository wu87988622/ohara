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
import { from } from 'rxjs';
import { concatMap, map } from 'rxjs/operators';
import localForage from 'localforage';

import * as actions from 'store/actions';
import { catchErrorWithEventLog } from '../utils';

localForage.config({
  name: 'ohara',
  storeName: 'event_logs',
});

const setLogToLocalForge$ = (values) => {
  const now = new Date();
  const key = now.getTime().toString();
  const value = {
    key,
    type: values.type,
    title: values.title,
    createAt: now,
    payload: values.data,
  };
  return from(localForage.setItem(key, value));
};

export default (action$) =>
  action$.pipe(
    ofType(actions.createEventLog.TRIGGER),
    map((action) => action.payload),
    // we should not omit any event
    concatMap((values) =>
      setLogToLocalForge$(values).pipe(
        concatMap((entity) =>
          from([
            actions.createEventLog.success(entity),
            // Trigger snackbar also
            actions.showMessage.trigger(entity.title),
            // update notification
            actions.updateNotifications.trigger(entity),
          ]),
        ),
        catchErrorWithEventLog((err) => actions.createEventLog.failure(err)),
      ),
    ),
  );
