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
import { defer } from 'rxjs';
import { map, startWith, mergeMap } from 'rxjs/operators';

import * as volumeApi from 'api/volumeApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';
import { catchErrorWithEventLog } from '../utils';

export const updateVolume$ = (values, resolve, reject) => {
  const volumeId = getId(values);
  return defer(() => volumeApi.update(values)).pipe(
    map((res) => res.data),
    map((data) => normalize(data, schema.volume)),
    map((normalizedData) => merge(normalizedData, { volumeId })),
    map((normalizedData) => {
      if (resolve) resolve(normalizedData);
      return actions.updateVolume.success(normalizedData);
    }),
    startWith(actions.updateVolume.request({ volumeId })),
    catchErrorWithEventLog((err) => {
      if (reject) reject(err);
      return actions.updateVolume.failure(
        merge(err, { volumeId: getId(values) }),
      );
    }),
  );
};

export default (action$) =>
  action$.pipe(
    ofType(actions.updateVolume.TRIGGER),
    map((action) => action.payload),
    mergeMap(({ values, resolve, reject }) =>
      updateVolume$(values, resolve, reject),
    ),
  );
