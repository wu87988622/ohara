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

import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useReducer,
  useCallback,
} from 'react';
import PropTypes from 'prop-types';
import { isEmpty, isEqual } from 'lodash';

import { useSnackbar } from 'context/SnackbarContext';
import { useWorkerState, useBrokerState, useZookeeperState } from 'context';
import { usePrevious } from 'utils/hooks';
import {
  fetchWorkspacesCreator,
  addWorkspaceCreator,
  updateWorkspaceCreator,
  stageWorkspaceCreator,
  deleteWorkspaceCreator,
} from './workspaceActions';
import { reducer, initialState } from './workspaceReducer';

const WorkspaceContext = createContext();
const WorkspaceStateContext = createContext();
const WorkspaceDispatchContext = createContext();

const WorkspaceProvider = ({ children }) => {
  const [state, dispatch] = useReducer(reducer, initialState);
  const { data: workspaces, isFetching } = state;
  const { data: workers } = useWorkerState();
  const { data: brokers } = useBrokerState();
  const { data: zookeepers } = useZookeeperState();

  /**
   * Short terms:
   * workspace => ws
   * worker => wk
   * broker => bk
   * zookeeper => zk
   */
  const [name, setName] = useState(null);
  const [currWs, setCurrWs] = useState(null);
  const [currWk, setCurrWk] = useState(null);
  const [currBk, setCurrBk] = useState(null);
  const [currZk, setCurrZk] = useState(null);
  const prevWs = usePrevious(currWs);
  const prevWk = usePrevious(currWk);
  const prevBk = usePrevious(currBk);
  const prevZk = usePrevious(currZk);

  const showMessage = useSnackbar();

  const fetchWorkspaces = useCallback(
    fetchWorkspacesCreator(state, dispatch, showMessage),
    [state],
  );

  useEffect(() => {
    fetchWorkspaces();
  }, [fetchWorkspaces]);

  // Set the current workspace
  useEffect(() => {
    if (!name || isEmpty(workspaces)) return;
    const found = workspaces.find(ws => ws.settings.name === name);
    if (!isEqual(found, prevWs)) setCurrWs(found);
  }, [name, workspaces, prevWs]);

  // Set the current worker
  useEffect(() => {
    if (!name || isEmpty(workers)) return;
    const found = workers.find(wk => wk.settings.name === name);
    if (!isEqual(found, prevWk)) setCurrWk(found);
  }, [name, workers, prevWk]);

  // Set the current broker
  useEffect(() => {
    if (!name || isEmpty(brokers)) return;
    const found = brokers.find(bk => bk.settings.name === name);
    if (!isEqual(found, prevBk)) setCurrBk(found);
  }, [name, brokers, prevBk]);

  // Set the current zookeeper
  useEffect(() => {
    if (!name || isEmpty(zookeepers)) return;
    const found = zookeepers.find(zk => zk.settings.name === name);
    if (!isEqual(found, prevZk)) setCurrZk(found);
  }, [name, zookeepers, prevZk]);

  const findWorkerByWorkspaceName = name =>
    workers.find(wk => wk.settings.name === name);

  return (
    <WorkspaceStateContext.Provider value={state}>
      <WorkspaceDispatchContext.Provider value={dispatch}>
        <WorkspaceContext.Provider
          value={{
            workspaces,
            currentWorkspace: currWs,
            currentWorker: currWk,
            currentBroker: currZk,
            currentZookeeper: currZk,
            isFetching,
            workspaceName: name,
            setWorkspaceName: setName,
            findByWorkspaceName: findWorkerByWorkspaceName,
          }}
        >
          {children}
        </WorkspaceContext.Provider>
      </WorkspaceDispatchContext.Provider>
    </WorkspaceStateContext.Provider>
  );
};

const useWorkspace = () => {
  const context = useContext(WorkspaceContext);

  if (context === undefined) {
    throw new Error('useWorkspace must be used within a WorkspaceProvider');
  }

  return context;
};

const useWorkspaceState = () => {
  const context = React.useContext(WorkspaceStateContext);
  if (context === undefined) {
    throw new Error(
      'useWorkspaceState must be used within a WorkspaceProvider',
    );
  }
  return context;
};

const useWorkspaceDispatch = () => {
  const context = React.useContext(WorkspaceDispatchContext);
  if (context === undefined) {
    throw new Error(
      'useWorkspaceDispatch must be used within a WorkspaceProvider',
    );
  }
  return context;
};

WorkspaceProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

const useWorkspaceActions = () => {
  const state = useWorkspaceState();
  const dispatch = useWorkspaceDispatch();
  const showMessage = useSnackbar();
  return {
    addWorkspace: addWorkspaceCreator(state, dispatch, showMessage),
    updateWorkspace: updateWorkspaceCreator(state, dispatch, showMessage),
    stageWorkspace: stageWorkspaceCreator(state, dispatch, showMessage),
    deleteWorkspace: deleteWorkspaceCreator(state, dispatch, showMessage),
  };
};

export {
  WorkspaceProvider,
  useWorkspace,
  useWorkspaceState,
  useWorkspaceDispatch,
  useWorkspaceActions,
};
