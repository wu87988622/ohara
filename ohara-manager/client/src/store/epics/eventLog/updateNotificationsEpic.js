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
import { infoKey, errorKey } from './const';

const increaseNotification$ = (entity, state$) => {
  const info = parseInt(localStorage.getItem(infoKey)) || 0;
  const error = parseInt(localStorage.getItem(errorKey)) || 0;
  const { limit, unlimited } = state$.value.entities.eventLogs.settings.data;

  // update the state by local storage only
  if (isEmpty(entity)) return of({ error, info });

  const nextCount = entity.type === LOG_LEVEL.info ? info + 1 : error + 1;
  const countToUpdate = unlimited ? nextCount : min([nextCount, limit]);
  if (entity.type === LOG_LEVEL.info) {
    localStorage.setItem(infoKey, countToUpdate);
    return of({ error, info: countToUpdate });
  } else {
    localStorage.setItem(errorKey, countToUpdate);
    return of({ error: countToUpdate, info });
  }
};

export default (action$, state$) =>
  action$.pipe(
    // we listen the create event epic
    ofType(actions.createEventLog.SUCCESS, actions.updateNotifications.TRIGGER),
    map(action => action.payload),
    concatMap(entity =>
      increaseNotification$(entity, state$).pipe(
        map(data => actions.updateNotifications.success(data)),
        catchError(res => of(actions.updateNotifications.failure(res))),
      ),
    ),
  );
