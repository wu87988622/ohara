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

/* eslint-disable no-throw-literal */
import {
  distinctUntilChanged,
  map,
  mergeMap,
  startWith,
  tap,
  concatAll,
  last,
} from 'rxjs/operators';
import { ofType } from 'redux-observable';
import { normalize } from 'normalizr';
import { merge } from 'lodash';

import * as actions from 'store/actions';
import * as volumeApi from 'api/volumeApi';
import * as schema from 'store/schema';
import { getId } from 'utils/object';
import { catchErrorWithEventLog } from '../utils';
import { defer, of } from 'rxjs';
import { isServiceStopped } from 'observables/utils';
import { retryBackoff } from 'backoff-rxjs';
import { RETRY_STRATEGY } from 'const';

export default (action$) =>
  action$.pipe(
    ofType(actions.stopVolume.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap(({ values, resolve, reject }) => {
      const volumeId = getId(values);
      return of(
        defer(() => volumeApi.stop(values)),
        defer(() => volumeApi.get(values)).pipe(
          tap((res) => {
            if (!isServiceStopped(res.data)) {
              throw {
                ...res,
                title: `Failed to stop volume ${values.name}: Unable to confirm the status of the volume is not running`,
              };
            }
          }),
          retryBackoff(RETRY_STRATEGY),
        ),
      ).pipe(
        concatAll(),
        last(),
        map((res) => {
          if (resolve) resolve(res);
          const normalizedData = merge(normalize(res.data, schema.volume), {
            volumeId,
          });
          return actions.stopVolume.success(normalizedData);
        }),
        startWith(actions.stopVolume.request({ volumeId })),
        catchErrorWithEventLog((err) => {
          if (reject) reject(err);
          return actions.stopVolume.failure(merge(err, { volumeId }));
        }),
      );
    }),
  );
