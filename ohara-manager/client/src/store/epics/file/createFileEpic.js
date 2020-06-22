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
import { ofType } from 'redux-observable';
import { from, defer } from 'rxjs';
import {
  catchError,
  map,
  startWith,
  distinctUntilChanged,
  concatMap,
} from 'rxjs/operators';

import * as fileApi from 'api/fileApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { LOG_LEVEL } from 'const';
import { isEmpty } from 'lodash';

const rename$ = (values) => {
  const { name, group } = values;
  const filenameExtension = name.split('.').pop();
  const fileName = name.replace('.' + filenameExtension, '');
  let index = 1;
  let finalName;

  const renameLoop = (files) => {
    if (
      isEmpty(
        files.find(
          (file) => file.name === fileName + index + '.' + filenameExtension,
        ),
      )
    ) {
      finalName = fileName + index + '.' + filenameExtension;
      return;
    }
    index++;
    renameLoop(files);
  };

  return defer(() => fileApi.getAll()).pipe(
    map((res) => res.data),
    map((files) => {
      if (
        isEmpty(
          files.find((file) => file.name === name && file.group === group),
        )
      ) {
        return values;
      }
      renameLoop(files);
      return {
        ...values,
        name: finalName,
        file: new File([values.file], finalName, {
          type: values.file.type,
        }),
      };
    }),
  );
};

export default (action$) => {
  return action$.pipe(
    ofType(actions.createFile.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    concatMap((values) =>
      rename$(values).pipe(
        concatMap((values) =>
          defer(() => fileApi.create(values)).pipe(
            map((res) => normalize(res.data, schema.file)),
            map((normalizedData) => actions.createFile.success(normalizedData)),
            startWith(actions.createFile.request()),
            catchError((err) =>
              from([
                actions.createFile.failure(err),
                actions.createEventLog.trigger({
                  ...err,
                  type: LOG_LEVEL.error,
                }),
              ]),
            ),
          ),
        ),
      ),
    ),
  );
};
