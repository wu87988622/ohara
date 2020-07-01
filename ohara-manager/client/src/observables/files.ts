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

import { Observable, defer, forkJoin, of } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import { isEmpty } from 'lodash';

import { ObjectKey } from 'api/apiInterface/basicInterface';
import * as fileApi from 'api/fileApi';
import { Data as FileData } from 'api/apiInterface/fileInterface';
import { getKey } from 'utils/object';
import { hashByGroupAndName } from 'utils/sha';

export function fetchFiles(workspaceKey: ObjectKey): Observable<FileData[]> {
  const fileGroup = hashByGroupAndName(workspaceKey.group, workspaceKey.name);
  return defer(() => fileApi.getAll({ group: fileGroup })).pipe(
    map((res) => res.data),
  );
}

export function deleteFile(key: ObjectKey) {
  return defer(() => fileApi.remove(key));
}

// Fetch and delete all files for this workspace
export function fetchAndDeleteFiles(workspaceKey: ObjectKey) {
  return fetchFiles(workspaceKey).pipe(
    mergeMap((files) => {
      if (isEmpty(files)) return of(files);
      return forkJoin(files.map((file) => deleteFile(getKey(file))));
    }),
  );
}
