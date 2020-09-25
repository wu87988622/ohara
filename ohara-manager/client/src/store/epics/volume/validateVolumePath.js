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
import { merge } from 'lodash';
import { ofType } from 'redux-observable';
import { defer, of, from } from 'rxjs';
import {
  map,
  startWith,
  distinctUntilChanged,
  mergeMap,
  concatAll,
  last,
  tap,
  catchError,
} from 'rxjs/operators';

import * as volumeApi from 'api/volumeApi';
import * as actions from 'store/actions';
import { serviceName } from 'utils/generate';
import { isServiceStarted, isServiceStopped } from 'observables/utils';
import { retryBackoff } from 'backoff-rxjs';
import { RETRY_STRATEGY } from 'const';

const validateVolumePath$ = (values) => {
  values = { ...values, name: serviceName(), group: 'default' };
  const volumeId = 'tmp_volume';
  return of(
    defer(() => volumeApi.create(values)),
    defer(() => volumeApi.start(values)),
    defer(() => volumeApi.get(values)).pipe(
      tap((res) => {
        if (!isServiceStarted(res.data)) {
          throw {
            ...res,
            title: `Failed to start volume ${values.name}: Unable to confirm the status of the volume is running`,
          };
        }
      }),
      retryBackoff(RETRY_STRATEGY),
    ),
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
    map(() => actions.validateVolumePath.success(merge(values, { volumeId }))),
    startWith(actions.validateVolumePath.request({ volumeId })),
    catchError((err) => {
      return from([
        actions.deleteVolume.trigger(values),
        actions.validateVolumePath.failure(merge(err, { volumeId })),
      ]);
    }),
  );
};

export default (action$) =>
  action$.pipe(
    ofType(actions.validateVolumePath.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap(({ values }) => validateVolumePath$(values)),
  );
