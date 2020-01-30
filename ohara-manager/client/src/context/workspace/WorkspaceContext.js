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
import { isEmpty, isEqual } from 'lodash';

import * as context from 'context';
import { useEventLog, usePrevious } from 'utils/hooks';
import { createActions } from './workspaceActions';
import { reducer, initialState } from './workspaceReducer';

const WorkspaceContext = React.createContext();
const WorkspaceStateContext = React.createContext();
const WorkspaceDispatchContext = React.createContext();

const WorkspaceProvider = ({ children }) => {
  const [state, dispatch] = React.useReducer(reducer, initialState);
  const { data: workspaces, isFetching } = state;

  const { workspaceName, pipelineName } = context.useApp();
  const { data: workers } = context.useWorkerState();
  const { data: brokers } = context.useBrokerState();
  const { data: zookeepers } = context.useZookeeperState();
  const { data: pipelines } = context.usePipelineState();
  const { workspaceApi } = context.useApi();
  const eventLog = useEventLog();

  /**
   * Abbreviations:
   * workspace => ws
   * worker => wk
   * broker => bk
   * zookeeper => zk
   * pipeline => pi
   */

  const [currWs, setCurrWs] = React.useState(null);
  const [currWk, setCurrWk] = React.useState(null);
  const [currBk, setCurrBk] = React.useState(null);
  const [currZk, setCurrZk] = React.useState(null);
  const [currPi, setCurrPi] = React.useState(null);
  const prevWs = usePrevious(currWs);
  const prevWk = usePrevious(currWk);
  const prevBk = usePrevious(currBk);
  const prevZk = usePrevious(currZk);
  const prevPi = usePrevious(currPi);

  React.useEffect(() => {
    if (!workspaceApi) return;
    const actions = createActions({ state, dispatch, eventLog, workspaceApi });
    actions.fetchWorkspaces();
  }, [state, dispatch, eventLog, workspaceApi]);

  // Set current pipeline
  React.useEffect(() => {
    if (!pipelineName || isEmpty(pipelines)) return setCurrPi(null); // Ensure the current pipeline is cleaned up when there's no pipeline available
    const found = pipelines.find(pi => pi.name === pipelineName);
    if (!isEqual(found, prevPi)) setCurrPi(found);
  }, [pipelineName, pipelines, prevPi, workspaces]);

  // Set the current workspace
  React.useEffect(() => {
    if (!workspaceName || isEmpty(workspaces)) return;
    const found = workspaces.find(ws => ws.name === workspaceName);
    if (!isEqual(found, prevWs)) setCurrWs(found);
  }, [workspaceName, workspaces, prevWs]);

  // Set the current worker
  React.useEffect(() => {
    if (!workspaceName || isEmpty(workers)) return;
    const found = workers.find(wk => wk.name === workspaceName);
    if (!isEqual(found, prevWk)) setCurrWk(found);
  }, [workspaceName, workers, prevWk]);

  // Set the current broker
  React.useEffect(() => {
    if (!workspaceName || isEmpty(brokers)) return;
    const found = brokers.find(bk => bk.name === workspaceName);
    if (!isEqual(found, prevBk)) setCurrBk(found);
  }, [workspaceName, brokers, prevBk]);

  // Set the current zookeeper
  React.useEffect(() => {
    if (!workspaceName || isEmpty(zookeepers)) return;
    const found = zookeepers.find(zk => zk.name === workspaceName);
    if (!isEqual(found, prevZk)) setCurrZk(found);
  }, [workspaceName, zookeepers, prevZk]);

  return (
    <WorkspaceStateContext.Provider value={state}>
      <WorkspaceDispatchContext.Provider value={dispatch}>
        <WorkspaceContext.Provider
          value={{
            workspaces,
            currentWorkspace: currWs,
            currentWorker: currWk,
            currentBroker: currBk,
            currentZookeeper: currZk,
            currentPipeline: currPi,
            isFetching,
            workspaceName,
          }}
        >
          {children}
        </WorkspaceContext.Provider>
      </WorkspaceDispatchContext.Provider>
    </WorkspaceStateContext.Provider>
  );
};

const useWorkspace = () => {
  const context = React.useContext(WorkspaceContext);
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
  const eventLog = useEventLog();
  const { workspaceApi } = context.useApi();
  return React.useMemo(
    () => createActions({ state, dispatch, eventLog, workspaceApi }),
    [state, dispatch, eventLog, workspaceApi],
  );
};

export {
  WorkspaceProvider,
  useWorkspace,
  useWorkspaceState,
  useWorkspaceDispatch,
  useWorkspaceActions,
};
