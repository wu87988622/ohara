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

import { useApp, useApi } from 'context';
import { initializeRoutine } from './logRoutines';
import { createActions } from './logActions';
import { reducer, initialState } from './logReducer';

const LogStateContext = React.createContext();
const LogDispatchContext = React.createContext();

const LogProvider = ({ children }) => {
  const [state, dispatch] = React.useReducer(reducer, initialState);
  const { workspaceName } = useApp();

  React.useEffect(() => {
    dispatch(initializeRoutine.trigger());
  }, [workspaceName]);

  return (
    <LogStateContext.Provider value={state}>
      <LogDispatchContext.Provider value={dispatch}>
        {children}
      </LogDispatchContext.Provider>
    </LogStateContext.Provider>
  );
};

const useLogState = () => {
  const context = React.useContext(LogStateContext);
  if (context === undefined) {
    throw new Error('useLogState must be used within a LogProvider');
  }
  return context;
};

const useLogDispatch = () => {
  const context = React.useContext(LogDispatchContext);
  if (context === undefined) {
    throw new Error('useLogDispatch must be used within a LogProvider');
  }
  return context;
};

LogProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

const useLogActions = () => {
  const state = useLogState();
  const dispatch = useLogDispatch();
  const { logApi } = useApi();
  return React.useMemo(() => createActions({ state, dispatch, logApi }), [
    state,
    dispatch,
    logApi,
  ]);
};

export { LogProvider, useLogState, useLogDispatch, useLogActions };
