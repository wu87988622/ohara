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

import { useCallback, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import * as actions from 'store/actions';
import { DialogName, DialogToggleType } from 'const';

function getActionByDialogName(dialogName) {
  switch (dialogName) {
    case DialogName.DEV_TOOL_DIALOG:
      return actions.openDevToolDialog;
    case DialogName.EVENT_LOG_DIALOG:
      return actions.openEventLogDialog;
    case DialogName.INTRO_DIALOG:
      return actions.openIntroDialog;
    case DialogName.NODE_LIST_DIALOG:
      return actions.openNodeListDialog;
    case DialogName.PIPELINE_PROPERTY_DIALOG:
      return actions.openPipelinePropertyDialog;
    case DialogName.WORKSPACE_CREATION_DIALOG:
      return actions.openWorkspaceCreationDialog;
    case DialogName.WORKSPACE_DELETE_DIALOG:
      return actions.openWorkspaceDeleteDialog;
    case DialogName.WORKSPACE_LIST_DIALOG:
      return actions.openWorkspaceListDialog;
    case DialogName.WORKSPACE_RESTART_DIALOG:
      return actions.openWorkspaceRestartDialog;
    case DialogName.WORKSPACE_SETTINGS_DIALOG:
      return actions.openWorkspaceSettingsDialog;
    default:
      return null;
  }
}

function useDialog(dialogName) {
  const dispatch = useDispatch();
  const action = getActionByDialogName(dialogName);
  const dialogState = useSelector((state) => state.ui.dialog[dialogName] || {});

  const openDialog = useCallback(
    (data) => {
      dispatch(action.trigger(data));
    },
    [action, dispatch],
  );

  const closeDialog = useCallback(() => {
    dispatch(action.fulfill());
  }, [action, dispatch]);

  const toggleDialog = useCallback(
    (toggleType = DialogToggleType.NORMAL) => {
      const { isOpen, data } = dialogState;
      if (toggleType === DialogToggleType.FORCE_OPEN) {
        dispatch(action.trigger(data));
      } else if (toggleType === DialogToggleType.FORCE_CLOSE) {
        dispatch(action.fulfill(data));
      } else {
        dispatch(isOpen ? action.fulfill(data) : action.trigger(data));
      }
    },
    [action, dialogState, dispatch],
  );

  return useMemo(
    () => ({
      // open dialog and overwrite data
      open: openDialog,
      // close dialog and clear data
      close: closeDialog,
      // open or close dialog without clearing the previous data
      toggle: toggleDialog,
      // dialog state
      isOpen: dialogState?.isOpen,
      // dialog state
      data: dialogState?.data,
      // dialog state
      lastUpdated: dialogState?.lastUpdated,
    }),
    [dialogState, openDialog, closeDialog, toggleDialog],
  );
}

export const useDevToolDialog = () => useDialog(DialogName.DEV_TOOL_DIALOG);
export const useEventLogDialog = () => useDialog(DialogName.EVENT_LOG_DIALOG);
export const useIntroDialog = () => useDialog(DialogName.INTRO_DIALOG);
export const useNodeListDialog = () => useDialog(DialogName.NODE_LIST_DIALOG);
export const usePipelinePropertyDialog = () =>
  useDialog(DialogName.PIPELINE_PROPERTY_DIALOG);
export const useWorkspaceCreationDialog = () =>
  useDialog(DialogName.WORKSPACE_CREATION_DIALOG);
export const useWorkspaceDeleteDialog = () =>
  useDialog(DialogName.WORKSPACE_DELETE_DIALOG);
export const useWorkspaceListDialog = () =>
  useDialog(DialogName.WORKSPACE_LIST_DIALOG);
export const useWorkspaceRestartDialog = () =>
  useDialog(DialogName.WORKSPACE_RESTART_DIALOG);
export const useWorkspaceSettingsDialog = () =>
  useDialog(DialogName.WORKSPACE_SETTINGS_DIALOG);
