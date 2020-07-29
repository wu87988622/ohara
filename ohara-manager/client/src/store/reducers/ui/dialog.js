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

import * as actions from 'store/actions';
import { isEqual as isDeepEqual } from 'lodash';
import { DialogName, DevToolTabName } from 'const';

const initialDialogState = {
  isOpen: false,
  data: null,
  lastUpdated: null,
};

const initialDevToolDialogState = {
  ...initialDialogState,
  data: { tabName: DevToolTabName.TOPIC },
};

const initialState = {
  [DialogName.DEV_TOOL_DIALOG]: initialDevToolDialogState,
  [DialogName.EVENT_LOG_DIALOG]: initialDialogState,
  [DialogName.INTRO_DIALOG]: initialDialogState,
  [DialogName.NODE_LIST_DIALOG]: initialDialogState,
  [DialogName.PIPELINE_PROPERTY_DIALOG]: initialDialogState,
  [DialogName.WORKSPACE_CREATION_DIALOG]: initialDialogState,
  [DialogName.WORKSPACE_DELETE_DIALOG]: initialDialogState,
  [DialogName.WORKSPACE_LIST_DIALOG]: initialDialogState,
  [DialogName.WORKSPACE_RESTART_DIALOG]: initialDialogState,
  [DialogName.WORKSPACE_SETTINGS_DIALOG]: initialDialogState,
};

// open the dialog
function openDialog(state, action, dialogName) {
  const dialogState = state[dialogName];
  const { data, lastUpdated, isOpen } = dialogState || {};
  const { payload: newData } = action;
  if (lastUpdated && isOpen && isDeepEqual(data, newData)) return state;

  return {
    ...state,
    [dialogName]: {
      ...dialogState,
      isOpen: true,
      data: newData,
      lastUpdated: new Date(),
    },
  };
}

// close the dialog
function closeDialog(state, action, dialogName) {
  const dialogState = state[dialogName];
  const { lastUpdated, isOpen } = dialogState || {};
  if (lastUpdated && !isOpen) return state;

  return {
    ...state,
    [dialogName]: {
      ...dialogState,
      isOpen: false,
      data: action.payload,
      lastUpdated: new Date(),
    },
  };
}

export default function reducer(state = initialState, action) {
  switch (action.type) {
    // toggle the devTool dialog
    case actions.openDevToolDialog.TRIGGER:
      return openDialog(state, action, DialogName.DEV_TOOL_DIALOG);
    case actions.openDevToolDialog.FULFILL:
      return closeDialog(state, action, DialogName.DEV_TOOL_DIALOG);

    // toggle the eventLog dialog
    case actions.openEventLogDialog.TRIGGER:
      return openDialog(state, action, DialogName.EVENT_LOG_DIALOG);
    case actions.openEventLogDialog.FULFILL:
      return closeDialog(state, action, DialogName.EVENT_LOG_DIALOG);

    // toggle the intro dialog
    case actions.openIntroDialog.TRIGGER:
      return openDialog(state, action, DialogName.INTRO_DIALOG);
    case actions.openIntroDialog.FULFILL:
      return closeDialog(state, action, DialogName.INTRO_DIALOG);

    // toggle the node list dialog
    case actions.openNodeListDialog.TRIGGER:
      return openDialog(state, action, DialogName.NODE_LIST_DIALOG);
    case actions.openNodeListDialog.FULFILL:
      return closeDialog(state, action, DialogName.NODE_LIST_DIALOG);

    // toggle the pipeline property dialog
    case actions.openPipelinePropertyDialog.TRIGGER:
      return openDialog(state, action, DialogName.PIPELINE_PROPERTY_DIALOG);
    case actions.openPipelinePropertyDialog.FULFILL:
      return closeDialog(state, action, DialogName.PIPELINE_PROPERTY_DIALOG);

    // toggle the workspace creation dialog
    case actions.openWorkspaceCreationDialog.TRIGGER:
      return openDialog(state, action, DialogName.WORKSPACE_CREATION_DIALOG);
    case actions.openWorkspaceCreationDialog.FULFILL:
      return closeDialog(state, action, DialogName.WORKSPACE_CREATION_DIALOG);

    // toggle the workspace delete dialog
    case actions.openWorkspaceDeleteDialog.TRIGGER:
      return openDialog(state, action, DialogName.WORKSPACE_DELETE_DIALOG);
    case actions.openWorkspaceDeleteDialog.FULFILL:
      return closeDialog(state, action, DialogName.WORKSPACE_DELETE_DIALOG);

    // toggle the workspace list dialog
    case actions.openWorkspaceListDialog.TRIGGER:
      return openDialog(state, action, DialogName.WORKSPACE_LIST_DIALOG);
    case actions.openWorkspaceListDialog.FULFILL:
      return closeDialog(state, action, DialogName.WORKSPACE_LIST_DIALOG);

    // toggle the workspace restart dialog
    case actions.openWorkspaceRestartDialog.TRIGGER:
      return openDialog(state, action, DialogName.WORKSPACE_RESTART_DIALOG);
    case actions.openWorkspaceRestartDialog.FULFILL:
      return closeDialog(state, action, DialogName.WORKSPACE_RESTART_DIALOG);

    // toggle the workspace settings dialog
    case actions.openWorkspaceSettingsDialog.TRIGGER:
      return openDialog(state, action, DialogName.WORKSPACE_SETTINGS_DIALOG);
    case actions.openWorkspaceSettingsDialog.FULFILL:
      return closeDialog(state, action, DialogName.WORKSPACE_SETTINGS_DIALOG);

    // after successfully deleting the workspace, the related dialogs should be closed
    case actions.deleteWorkspace.SUCCESS:
      return {
        ...state,
        [DialogName.PIPELINE_PROPERTY_DIALOG]: initialDialogState,
        [DialogName.WORKSPACE_CREATION_DIALOG]: initialDialogState,
        [DialogName.WORKSPACE_RESTART_DIALOG]: initialDialogState,
        [DialogName.WORKSPACE_SETTINGS_DIALOG]: initialDialogState,
      };

    default:
      return state;
  }
}
