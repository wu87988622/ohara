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
import { of, defer, iif, throwError, zip } from 'rxjs';
import {
  catchError,
  map,
  startWith,
  retryWhen,
  delay,
  concatMap,
  distinctUntilChanged,
  mergeMap,
} from 'rxjs/operators';

import * as shabondiApi from 'api/shabondiApi';
import * as actions from 'store/actions';
import { CELL_STATUS } from 'const';
import { getId } from 'utils/object';

const deleteShabondi$ = values => {
  const { params, options } = values;
  const { paperApi } = options;
  const shabondiId = getId(params);
  paperApi.updateElement(params.id, {
    status: CELL_STATUS.pending,
  });
  return zip(
    defer(() => shabondiApi.remove(params)),
    defer(() => shabondiApi.getAll({ group: params.group })).pipe(
      map(res => {
        if (res.data.find(shabondi => shabondi.name === params.name)) throw res;
        else return res.data;
      }),
      retryWhen(errors =>
        errors.pipe(
          concatMap((value, index) =>
            iif(
              () => index > 4,
              throwError('exceed max retry times'),
              of(value).pipe(delay(2000)),
            ),
          ),
        ),
      ),
    ),
  ).pipe(
    map(() => {
      paperApi.removeElement(params.id);
      return actions.deleteShabondi.success({ shabondiId });
    }),
    startWith(actions.deleteShabondi.request({ shabondiId })),
    catchError(error => {
      paperApi.updateElement(params.id, {
        status: CELL_STATUS.failed,
      });
      return of(actions.deleteShabondi.failure(merge(error, { shabondiId })));
    }),
  );
};

export default action$ => {
  return action$.pipe(
    ofType(actions.deleteShabondi.TRIGGER),
    map(action => action.payload),
    distinctUntilChanged(),
    mergeMap(values => deleteShabondi$(values)),
  );
};
