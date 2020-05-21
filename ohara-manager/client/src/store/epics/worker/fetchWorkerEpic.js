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
import { sortBy, merge, mergeWith, omit, isArray } from 'lodash';
import { ofType } from 'redux-observable';
import { of, defer, forkJoin, iif, zip, from, throwError } from 'rxjs';
import {
  catchError,
  map,
  switchMap,
  startWith,
  throttleTime,
  retryWhen,
  concatMap,
  delay,
  takeUntil,
} from 'rxjs/operators';

import { LOG_LEVEL } from 'const';
import * as workerApi from 'api/workerApi';
import * as inspectApi from 'api/inspectApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';

const customizer = (objValue, srcValue) => {
  if (isArray(objValue)) {
    // combine the classInfos array and sort it by "displayed" className
    return sortBy(objValue.concat(srcValue), v => v.className.split('.').pop());
  }
};

const fetchWorker$ = params => {
  const workerId = getId(params);
  return forkJoin(
    defer(() => workerApi.get(params)).pipe(
      map(res => res.data),
      map(data => normalize(data, schema.worker)),
    ),
    zip(
      defer(() => inspectApi.getShabondiInfo()),
      defer(() => inspectApi.getWorkerInfo(params)).pipe(
        map(res => {
          // Ensure classInfos are loaded since it's required in our UI
          if (res.data.classInfos.length === 0) throw res;
          return res;
        }),
        retryWhen(errors =>
          errors.pipe(
            concatMap((value, index) =>
              iif(
                () => index > 10,
                throwError({
                  data: value?.data,
                  meta: value?.meta,
                  title:
                    `Try to fetch connector list from worker: "${params.name}" failed after retry ${index} times. ` +
                    `Expected classInfos is not Empty, Actual classInfos: ${value.data.classInfos}`,
                }),
                of(value).pipe(delay(2000)),
              ),
            ),
          ),
        ),
      ),
    ).pipe(
      map(([shabondiInfo, wkInfo]) =>
        mergeWith(
          wkInfo.data,
          // we only need to inject the shabondi classes into worker.classInfos
          // the other fields of inspect/shabondi should be omitted
          omit(shabondiInfo.data, ['settingDefinitions', 'imageName']),
          params,
          customizer,
        ),
      ),
      map(data => normalize(data, schema.info)),
    ),
  ).pipe(
    map(normalizedData => merge(...normalizedData, { workerId })),
    map(normalizedData => actions.fetchWorker.success(normalizedData)),
    startWith(actions.fetchWorker.request({ workerId })),
    catchError(err =>
      from([
        actions.fetchWorker.failure(merge(err, { workerId })),
        actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
      ]),
    ),
  );
};

export default action$ =>
  action$.pipe(
    ofType(actions.fetchWorker.TRIGGER),
    map(action => action.payload),
    throttleTime(1000),
    switchMap(params =>
      fetchWorker$(params).pipe(
        // Stop fetching worker info once the delete workspace action is triggered
        takeUntil(action$.pipe(ofType(actions.deleteWorkspace.TRIGGER))),
      ),
    ),
  );
