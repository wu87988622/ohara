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

import { createRoutine } from 'redux-routines';
import { DialogName } from 'const';

export const openDialog = createRoutine('OPEN_DIALOG');

export const openDevToolDialog = createRoutine(DialogName.DEV_TOOL_DIALOG);
export const openEventLogDialog = createRoutine(DialogName.EVENT_LOG_DIALOG);
export const openIntroDialog = createRoutine(DialogName.INTRO_DIALOG);
export const openNodeListDialog = createRoutine(DialogName.NODE_LIST_DIALOG);
export const openPipelinePropertyDialog = createRoutine(
  DialogName.PIPELINE_PROPERTY_DIALOG,
);
export const openWorkspaceCreationDialog = createRoutine(
  DialogName.WORKSPACE_CREATION_DIALOG,
);
export const openWorkspaceDeleteDialog = createRoutine(
  DialogName.WORKSPACE_DELETE_DIALOG,
);
export const openWorkspaceListDialog = createRoutine(
  DialogName.WORKSPACE_LIST_DIALOG,
);
export const openWorkspaceRestartDialog = createRoutine(
  DialogName.WORKSPACE_RESTART_DIALOG,
);
export const openWorkspaceSettingsDialog = createRoutine(
  DialogName.WORKSPACE_SETTINGS_DIALOG,
);
