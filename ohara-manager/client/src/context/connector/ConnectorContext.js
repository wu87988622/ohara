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
  useReducer,
  useContext,
  useEffect,
  useMemo,
} from 'react';
import PropTypes from 'prop-types';
import { useApi } from 'context';
import * as hooks from 'hooks';
import { createActions } from './connectorActions';
import { reducer, initialState } from './connectorReducer';
import { initializeRoutine } from './connectorRoutines';

const ConnectorStateContext = createContext();
const ConnectorDispatchContext = createContext();

const ConnectorProvider = ({ children }) => {
  const [state, dispatch] = useReducer(reducer, initialState);
  const eventLog = hooks.useEventLog();
  const { connectorApi } = useApi();
  const pipelineGroup = hooks.usePipelineGroup();
  const pipelineName = hooks.usePipelineName();

  React.useEffect(() => {
    dispatch(initializeRoutine.trigger());
  }, [pipelineName, pipelineGroup]);

  useEffect(() => {
    if (!connectorApi) return;
    const actions = createActions({ state, dispatch, eventLog, connectorApi });
    actions.fetchConnectors();
  }, [state, dispatch, eventLog, connectorApi]);

  return (
    <ConnectorStateContext.Provider value={state}>
      <ConnectorDispatchContext.Provider value={dispatch}>
        {children}
      </ConnectorDispatchContext.Provider>
    </ConnectorStateContext.Provider>
  );
};

ConnectorProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

const useConnectorState = () => {
  const context = useContext(ConnectorStateContext);
  if (context === undefined) {
    throw new Error(
      'useConnectorState must be used within a ConnectorProvider',
    );
  }
  return context;
};

const useConnectorDispatch = () => {
  const context = useContext(ConnectorDispatchContext);
  if (context === undefined) {
    throw new Error(
      'useConnectorDispatch must be used within a ConnectorProvider',
    );
  }
  return context;
};

const useConnectorActions = () => {
  const state = useConnectorState();
  const dispatch = useConnectorDispatch();
  const eventLog = hooks.useEventLog();
  const { connectorApi } = useApi();
  return useMemo(
    () => createActions({ state, dispatch, eventLog, connectorApi }),
    [state, dispatch, eventLog, connectorApi],
  );
};

export {
  ConnectorProvider,
  useConnectorState,
  useConnectorDispatch,
  useConnectorActions,
};
