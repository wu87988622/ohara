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

import { map } from 'rxjs/operators';
import { ObjectKey } from 'api/apiInterface/basicInterface';
import * as workspaceApi from 'api/workspaceApi';
import { deferApi } from './internal/deferApi';

export function createWorkspace(values: any) {
  return deferApi(() => workspaceApi.create(values)).pipe(
    map((res) => res.data),
  );
}

export function deleteWorkspace(key: ObjectKey) {
  return deferApi(() => workspaceApi.remove(key));
}
