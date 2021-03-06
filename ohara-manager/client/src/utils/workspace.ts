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

import { WorkspaceFlag, Workspace } from 'api/apiInterface/workspaceInterface';

export function isUnstable(workspace: Workspace): boolean {
  if (!workspace) return true;
  return (
    workspace.flag === WorkspaceFlag.CREATING ||
    workspace.flag === WorkspaceFlag.RESTARTING ||
    workspace.flag === WorkspaceFlag.DELETING
  );
}

export function isStable(workspace: Workspace): boolean {
  if (!workspace) return false;
  return !isUnstable(workspace);
}
