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

import { min } from 'lodash';
import { ofType } from 'redux-observable';
import { of } from 'rxjs';
import { switchMap, map, catchError } from 'rxjs/operators';

import * as actions from 'store/actions';
import { LOG_LEVEL } from 'const';

const increaseNotification$ = (type, state$) => {
  const { error, info } = state$.value.entities.eventLogs.notifications.data;
  const { limit, unlimited } = state$.value.entities.eventLogs.settings.data;

  const nextCount = type === LOG_LEVEL.info ? info + 1 : error + 1;
  const countToUpdate = unlimited ? nextCount : min([nextCount, limit]);
  const ensuredData =
    type === LOG_LEVEL.info
      ? { error, info: countToUpdate }
      : { error: countToUpdate, info };
  return of(ensuredData);
};

export default (action$, state$) =>
  action$.pipe(
    // we listen the create event epic
    ofType(actions.createEventLog.SUCCESS),
    switchMap(entity =>
      increaseNotification$(entity.payload.type, state$).pipe(
        map(entity => actions.updateNotifications.success(entity)),
        catchError(res => of(actions.updateNotifications.failure(res))),
      ),
    ),
  );
