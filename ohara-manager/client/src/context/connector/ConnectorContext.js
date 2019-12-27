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

import React, { createContext, useReducer, useContext, useEffect } from 'react';
import PropTypes from 'prop-types';
import { useApi } from 'context';
import { createActions } from './connectorActions';
import { reducer, initialState } from './connectorReducer';

const ConnectorStateContext = createContext();
const ConnectorDispatchContext = createContext();

const ConnectorProvider = ({ children }) => {
  const [state, dispatch] = useReducer(reducer, initialState);
  const { connectorApi } = useApi();

  useEffect(() => {
    if (!connectorApi) return;
    const actions = createActions({ state, dispatch, connectorApi });
    actions.fetchConnectors();
  }, [state, connectorApi]);

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
  const { connectorApi } = useApi();
  return createActions({ state, dispatch, connectorApi });
};

export {
  ConnectorProvider,
  useConnectorState,
  useConnectorDispatch,
  useConnectorActions,
};
