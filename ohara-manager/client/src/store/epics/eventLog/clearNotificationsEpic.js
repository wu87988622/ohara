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
import { of, from } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

import * as actions from 'store/actions';
import { errorKey, warningKey } from './const';

export default (action$) =>
  action$.pipe(
    ofType(actions.clearNotifications.TRIGGER),
    switchMap(() => {
      localStorage.removeItem(warningKey);
      localStorage.removeItem(errorKey);
      return from([
        actions.clearNotifications.success(),
        // we need to re-fetch the notification for AppBar
        actions.updateNotifications.trigger(),
      ]);
    }),
    catchError((res) => of(actions.clearNotifications.failure(res))),
  );
