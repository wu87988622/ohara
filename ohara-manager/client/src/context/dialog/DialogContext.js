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

import * as actions from './dialogActions';
import * as dialogNames from './dialogNames';
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
    isDialogOpen: actions.isDialogOpenCreator(state, dispatch),
    openDialog: actions.openDialogCreator(state, dispatch),
    closeDialog: actions.closeDialogCreator(state, dispatch),
    toggleDialog: actions.toggleDialogCreator(state, dispatch),
    getDialogData: actions.getDialogDataCreator(state, dispatch),
    setDialogData: actions.setDialogDataCreator(state, dispatch),
    closePeakDialog: actions.closePeakDialogCreator(state, dispatch),
    closeAllDialog: actions.closeAllDialogCreator(state, dispatch),
  };
};

const createUseDialogActions = (name) => () => {
  const {
    isDialogOpen,
    openDialog,
    closeDialog,
    toggleDialog,
    getDialogData,
    setDialogData,
    closePeakDialog,
    closeAllDialog,
  } = useDialogActions();
  return {
    isOpen: isDialogOpen(name),
    open: (data) => openDialog(name, data),
    close: () => closeDialog(name),
    toggle: () => toggleDialog(name),
    data: getDialogData(name),
    setData: (data) => setDialogData(name, data),
    closePeak: closePeakDialog,
    closeAll: closeAllDialog,
  };
};

const useAddTopicDialog = createUseDialogActions(dialogNames.ADD_TOPIC_DIALOG);
const useViewTopicDialog = createUseDialogActions(
  dialogNames.VIEW_TOPIC_DIALOG,
);
const useListNodeDialog = createUseDialogActions(dialogNames.LIST_NODE_DIALOG);
const useViewNodeDialog = createUseDialogActions(dialogNames.VIEW_NODE_DIALOG);
const useEditNodeDialog = createUseDialogActions(dialogNames.EDIT_NODE_DIALOG);
const useAddNodeDialog = createUseDialogActions(dialogNames.ADD_NODE_DIALOG);
const useDevToolDialog = createUseDialogActions(dialogNames.DEV_TOOL_DIALOG);
const useEventLogDialog = createUseDialogActions(dialogNames.EVENT_LOG_DIALOG);
const useEventLogContentDialog = createUseDialogActions(
  dialogNames.EVENT_LOG_CONTENT_DIALOG,
);
const usePipelinePropertyDialog = createUseDialogActions(
  dialogNames.PIPELINE_PROPERTY_DIALOG,
);
const useListWorkspacesDialog = createUseDialogActions(
  dialogNames.LIST_WORKSPACES_DIALOG,
);
const useAddPluginDialog = createUseDialogActions(
  dialogNames.ADD_PLUGIN_DIALOG,
);

export {
  DialogProvider,
  // Workspace
  useListWorkspacesDialog,
  // Topic
  useAddTopicDialog,
  useViewTopicDialog,
  // Node
  useListNodeDialog,
  useViewNodeDialog,
  useEditNodeDialog,
  useAddNodeDialog,
  // Others
  useEventLogDialog,
  useEventLogContentDialog,
  useDevToolDialog,
  usePipelinePropertyDialog,
  useAddPluginDialog,
};
