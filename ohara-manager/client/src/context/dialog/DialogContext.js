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

import React from 'react';
import PropTypes from 'prop-types';

import {
  isDialogOpenCreator,
  openDialogCreator,
  closeDialogCreator,
  getDialogDataCreator,
  setDialogDataCreator,
  closePeakDialogCreator,
  closeAllDialogCreator,
} from './dialogActions';
import {
  EDIT_WORKSPACE_DIALOG,
  ADD_TOPIC_DIALOG,
  VIEW_TOPIC_DIALOG,
  DEV_TOOL_DIALOG,
  GRAPH_SETTING_DIALOG,
  LIST_WORKSPACES_DIALOG,
} from './dialogNames';
import { reducer, initialState } from './dialogReducer';

const DialogStateContext = React.createContext();
const DialogDispatchContext = React.createContext();

const DialogProvider = ({ children }) => {
  const [state, dispatch] = React.useReducer(reducer, initialState);

  return (
    <DialogStateContext.Provider value={state}>
      <DialogDispatchContext.Provider value={dispatch}>
        {children}
      </DialogDispatchContext.Provider>
    </DialogStateContext.Provider>
  );
};

const useDialogState = () => {
  const context = React.useContext(DialogStateContext);
  if (context === undefined) {
    throw new Error('useDialogState must be used within a DialogProvider');
  }
  return context;
};

const useDialogDispatch = () => {
  const context = React.useContext(DialogDispatchContext);
  if (context === undefined) {
    throw new Error('useDialogDispatch must be used within a DialogProvider');
  }
  return context;
};

DialogProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

const useDialogActions = () => {
  const state = useDialogState();
  const dispatch = useDialogDispatch();
  return {
    isDialogOpen: isDialogOpenCreator(state, dispatch),
    openDialog: openDialogCreator(state, dispatch),
    closeDialog: closeDialogCreator(state, dispatch),
    getDialogData: getDialogDataCreator(state, dispatch),
    setDialogData: setDialogDataCreator(state, dispatch),
    closePeakDialog: closePeakDialogCreator(state, dispatch),
    closeAllDialog: closeAllDialogCreator(state, dispatch),
  };
};

const createUseDialogActions = name => () => {
  const {
    isDialogOpen,
    openDialog,
    closeDialog,
    getDialogData,
    setDialogData,
    closePeakDialog,
    closeAllDialog,
  } = useDialogActions();
  return {
    isOpen: isDialogOpen(name),
    open: data => openDialog(name, data),
    close: () => closeDialog(name),
    data: getDialogData(name),
    setData: data => setDialogData(name, data),
    closePeak: closePeakDialog,
    closeAll: closeAllDialog,
  };
};

const useEditWorkspaceDialog = createUseDialogActions(EDIT_WORKSPACE_DIALOG);
const useAddTopicDialog = createUseDialogActions(ADD_TOPIC_DIALOG);
const useViewTopicDialog = createUseDialogActions(VIEW_TOPIC_DIALOG);
const useDevToolDialog = createUseDialogActions(DEV_TOOL_DIALOG);
const useGraphSettingDialog = createUseDialogActions(GRAPH_SETTING_DIALOG);
const useListWorkspacesDialog = createUseDialogActions(LIST_WORKSPACES_DIALOG);

export {
  DialogProvider,
  useEditWorkspaceDialog,
  useAddTopicDialog,
  useViewTopicDialog,
  useDevToolDialog,
  useGraphSettingDialog,
  useListWorkspacesDialog,
};
