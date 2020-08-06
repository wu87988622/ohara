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
import { sortBy, mergeWith, omit, isArray, merge } from 'lodash';
import { ofType } from 'redux-observable';
import { defer, zip } from 'rxjs';
import { map, switchMap, startWith, takeUntil } from 'rxjs/operators';

/* eslint-disable no-throw-literal */
import * as inspectApi from 'api/inspectApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';
import { retryBackoff } from 'backoff-rxjs';
import { RETRY_STRATEGY } from 'const';
import { catchErrorWithEventLog } from '../utils';

const customizer = (objValue, srcValue) => {
  if (isArray(objValue)) {
    // combine the classInfos array and sort it by "displayed" className
    return sortBy(objValue.concat(srcValue), (v) =>
      v.className.split('.').pop(),
    );
  }
};

const inspectWorker$ = (params) =>
  zip(
    defer(() => inspectApi.getShabondiInfo()),
    defer(() => inspectApi.getWorkerInfo(params)).pipe(
      map((res) => {
        // Ensure classInfos are loaded since it's required in our UI
        if (res.data.classInfos.length === 0) {
          throw {
            ...res,
            title: `Inspect worker ${params?.name} info failed.`,
          };
        }

        return res;
      }),
      retryBackoff(RETRY_STRATEGY),
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
    map((data) => normalize(data, schema.info)),
    map((normalizedData) => actions.inspectWorker.success(normalizedData)),
    startWith(actions.inspectWorker.request({ workerId: getId(params) })),
    catchErrorWithEventLog((err) =>
      actions.inspectWorker.failure(merge(err, { workerId: getId(params) })),
    ),
  );

export default (action$) =>
  action$.pipe(
    ofType(actions.inspectWorker.TRIGGER),
    map((action) => action.payload),
    switchMap((params) =>
      inspectWorker$(params).pipe(
        // Stop fetching worker info once the delete workspace action is triggered
        takeUntil(action$.pipe(ofType(actions.deleteWorkspace.TRIGGER))),
      ),
    ),
  );
