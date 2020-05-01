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
import { defer, from } from 'rxjs';
import { catchError, map, startWith, mergeMap } from 'rxjs/operators';

import * as brokerApi from 'api/brokerApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';
import { LOG_LEVEL } from 'const';

const updateBroker$ = values => {
  const brokerId = getId(values);
  return defer(() => brokerApi.update(values)).pipe(
    map(res => res.data),
    map(data => normalize(data, schema.broker)),
    map(normalizedData => merge(normalizedData, { brokerId })),
    map(normalizedData => actions.updateBroker.success(normalizedData)),
    startWith(actions.updateBroker.request({ brokerId })),
    catchError(err =>
      from([
        actions.updateBroker.failure(merge(err, { brokerId })),
        actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
      ]),
    ),
  );
};

export default action$ =>
  action$.pipe(
    ofType(actions.updateBroker.TRIGGER),
    map(action => action.payload),
    mergeMap(values => updateBroker$(values)),
  );
