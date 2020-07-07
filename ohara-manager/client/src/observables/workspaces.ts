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

import { defer } from 'rxjs';
import { map } from 'rxjs/operators';
import { ObjectKey } from 'api/apiInterface/basicInterface';
import { Workspace } from 'api/apiInterface/workspaceInterface';
import * as workspaceApi from 'api/workspaceApi';

export function createWorkspace(workspace: Workspace) {
  return defer(() => workspaceApi.create(workspace)).pipe(
    map((res) => res.data),
  );
}

export function deleteWorkspace(key: ObjectKey) {
  return defer(() => workspaceApi.remove(key));
}

export function fetchWorkspaces() {
  return defer(() => workspaceApi.getAll()).pipe(map((res) => res.data));
}

export function updateWorkspace(workspace: Workspace) {
  return defer(() => workspaceApi.update(workspace)).pipe(
    map((res) => res.data),
  );
}
