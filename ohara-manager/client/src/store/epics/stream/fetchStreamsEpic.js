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
import { from, of } from 'rxjs';
import {
  switchMap,
  map,
  startWith,
  catchError,
  concatAll,
} from 'rxjs/operators';

import * as streamApi from 'api/streamApi';
import * as fileApi from 'api/fileApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { KIND } from 'const';

export default action$ =>
  action$.pipe(
    ofType(actions.fetchStreams.TRIGGER),
    map(action => action.payload),
    switchMap(values => {
      return of(
        from(streamApi.get(values)).pipe(
          map(res => normalize(res.data, [schema.stream])),
          map(normalizedData => actions.fetchStreams.success(normalizedData)),
          startWith(actions.fetchStreams.request()),
          catchError(err => of(actions.fetchStreams.failure(err))),
        ),
        from(fileApi.getAll()).pipe(
          map(getStreams),
          map(files => normalizeInfos(values.group, files)),
          map(normalizedData =>
            actions.fetchStreamInfo.success(normalizedData),
          ),
          startWith(actions.fetchStreamInfo.request()),
          catchError(err => of(actions.fetchStreamInfo.failure(err))),
        ),
      ).pipe(concatAll());
    }),
  );

function getStreams(res) {
  return res.data.filter(file => {
    return file.classInfos.find(classInfo => {
      const { settingDefinitions: defs } = classInfo;
      const { defaultValue: kind } = defs.find(def => def.key === 'kind');
      return kind === KIND.stream;
    });
  });
}

function normalizeInfos(group, files) {
  const [results] = files.map(file => {
    const { classInfos } = file;
    return classInfos.map(classInfo => {
      const { className } = classInfo;
      return normalize({ name: className, classInfo, group }, schema.info);
    });
  });

  return results;
}
