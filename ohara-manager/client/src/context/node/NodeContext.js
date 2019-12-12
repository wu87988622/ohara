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
import { useSnackbar } from 'context/SnackbarContext';
import {
  fetchNodesCreator,
  addNodeCreator,
  updateNodeCreator,
  deleteNodeCreator,
} from './nodeActions';
import { reducer, initialState } from './nodeReducer';

const NodeStateContext = React.createContext();
const NodeDispatchContext = React.createContext();

const NodeProvider = ({ children }) => {
  const [state, dispatch] = React.useReducer(reducer, initialState);
  const showMessage = useSnackbar();

  const fetchNodes = React.useCallback(
    fetchNodesCreator(state, dispatch, showMessage),
    [state],
  );

  React.useEffect(() => {
    fetchNodes();
  }, [fetchNodes]);

  return (
    <NodeStateContext.Provider value={state}>
      <NodeDispatchContext.Provider value={dispatch}>
        {children}
      </NodeDispatchContext.Provider>
    </NodeStateContext.Provider>
  );
};

NodeProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

const useNodeState = () => {
  const context = React.useContext(NodeStateContext);
  if (context === undefined) {
    throw new Error('useNodeState must be used within a NodeProvider');
  }
  return context;
};

const useNodeDispatch = () => {
  const context = React.useContext(NodeDispatchContext);
  if (context === undefined) {
    throw new Error('useNodeDispatch must be used within a NodeProvider');
  }
  return context;
};

const useNodeActions = () => {
  const state = useNodeState();
  const dispatch = useNodeDispatch();
  const showMessage = useSnackbar();
  return {
    addNode: addNodeCreator(state, dispatch, showMessage),
    updateNode: updateNodeCreator(state, dispatch, showMessage),
    deleteNode: deleteNodeCreator(state, dispatch, showMessage),
  };
};

export { NodeProvider, useNodeState, useNodeDispatch, useNodeActions };
