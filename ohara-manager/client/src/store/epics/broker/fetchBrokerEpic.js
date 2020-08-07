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
import { merge, get } from 'lodash';
import { ofType } from 'redux-observable';
import { defer, forkJoin } from 'rxjs';
import { map, switchMap, startWith, throttleTime } from 'rxjs/operators';

import { UISettingDef, Type } from 'api/apiInterface/definitionInterface';
import * as brokerApi from 'api/brokerApi';
import * as inspectApi from 'api/inspectApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';
import { catchErrorWithEventLog } from '../utils';

const addSettingDefByKeyAndType = (...args) => new UISettingDef(...args);

const fetchBroker$ = (params) => {
  const brokerId = getId(params);
  return forkJoin(
    defer(() => brokerApi.get(params)).pipe(
      map((res) => res.data),
      map((data) => normalize(data, schema.broker)),
    ),
    defer(() => inspectApi.getBrokerInfo(params)).pipe(
      // add UI SettingDef to the broker info
      // Note: it is a workaround solution!
      map((res) => {
        get(res.data.classInfos, '[0].settingDefinitions', []).push(
          addSettingDefByKeyAndType('displayName', Type.STRING, false),
          addSettingDefByKeyAndType('isShared', Type.BOOLEAN),
        );
        return res;
      }),
      map((res) => merge(res.data, params)),
      map((data) => normalize(data, schema.info)),
    ),
  ).pipe(
    map((normalizedData) => merge(...normalizedData, { brokerId })),
    map((normalizedData) => actions.fetchBroker.success(normalizedData)),
    startWith(actions.fetchBroker.request({ brokerId })),
    catchErrorWithEventLog((err) =>
      actions.fetchBroker.failure(merge(err, { brokerId })),
    ),
  );
};

export default (action$) =>
  action$.pipe(
    ofType(actions.fetchBroker.TRIGGER),
    map((action) => action.payload),
    throttleTime(1000),
    switchMap((params) => fetchBroker$(params)),
  );
