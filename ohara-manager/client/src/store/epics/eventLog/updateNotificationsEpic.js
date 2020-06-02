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

import { min, isEmpty } from 'lodash';
import { ofType } from 'redux-observable';
import { of } from 'rxjs';
import { map, catchError, concatMap } from 'rxjs/operators';

import * as actions from 'store/actions';
import { LOG_LEVEL } from 'const';
import { errorKey, warningKey } from './const';

const increaseNotification$ = (entity, state$) => {
  const error = parseInt(localStorage.getItem(errorKey)) || 0;
  const warning = parseInt(localStorage.getItem(warningKey)) || 0;
  const { limit, unlimited } = state$.value.entities.eventLogs.settings.data;

  // update the state by local storage only
  if (isEmpty(entity) || entity?.type === LOG_LEVEL.info) {
    return of({ error, warning });
  }

  const isWarning = entity.type === LOG_LEVEL.warning;
  const nextCount = isWarning ? warning + 1 : error + 1;
  const countToUpdate = unlimited ? nextCount : min([nextCount, limit]);

  if (isWarning) {
    localStorage.setItem(warningKey, countToUpdate);
    return of({ warning: countToUpdate, error });
  }

  localStorage.setItem(errorKey, countToUpdate);
  return of({ error: countToUpdate, warning });
};

export default (action$, state$) =>
  action$.pipe(
    // we listen the create event epic
    ofType(actions.createEventLog.SUCCESS, actions.updateNotifications.TRIGGER),
    map((action) => action.payload),
    concatMap((entity) =>
      increaseNotification$(entity, state$).pipe(
        map((data) => actions.updateNotifications.success(data)),
        catchError((res) => of(actions.updateNotifications.failure(res))),
      ),
    ),
  );
