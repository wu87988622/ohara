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
import { normalize } from 'normalizr';
import { ofType } from 'redux-observable';
import { defer } from 'rxjs';
import { map, startWith, distinctUntilChanged, mergeMap } from 'rxjs/operators';

import { CELL_STATUS } from 'const';
import * as shabondiApi from 'api/shabondiApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';
import { catchErrorWithEventLog } from '../utils';

const createShabondi$ = (value) => {
  const { values, options } = value;
  const shabondiId = getId(values);
  return defer(() => shabondiApi.create(values)).pipe(
    map((res) => res.data),
    map((data) => normalize(data, schema.shabondi)),
    map((normalizedData) => {
      options.paperApi.updateElement(values.id, {
        status: CELL_STATUS.stopped,
      });
      return merge(normalizedData, { shabondiId });
    }),
    map((normalizedData) => actions.createShabondi.success(normalizedData)),
    startWith(actions.createShabondi.request({ shabondiId })),
    catchErrorWithEventLog((err) => {
      options.paperApi.removeElement(values.id);
      return actions.createShabondi.failure(merge(err, { shabondiId }));
    }),
  );
};

export default (action$) =>
  action$.pipe(
    ofType(actions.createShabondi.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap((value) => createShabondi$(value)),
  );
