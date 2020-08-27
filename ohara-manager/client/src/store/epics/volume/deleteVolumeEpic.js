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

import { merge } from 'lodash';
import { ofType } from 'redux-observable';
import { distinctUntilChanged, map, mergeMap, startWith } from 'rxjs/operators';

import * as volumeApi from 'api/volumeApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { catchErrorWithEventLog } from '../utils';
import { defer } from 'rxjs';

export default (action$) =>
  action$.pipe(
    ofType(actions.deleteVolume.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap(({ values, resolve, reject }) => {
      const volumeId = getId(values);
      return defer(() => volumeApi.remove(values)).pipe(
        map(() => {
          if (resolve) resolve(volumeId);
          return actions.deleteVolume.success({ volumeId });
        }),
        startWith(actions.deleteVolume.request({ volumeId })),
        catchErrorWithEventLog((err) => {
          if (reject) reject(err);
          return actions.deleteVolume.failure(merge(err, { volumeId }));
        }),
      );
    }),
  );
