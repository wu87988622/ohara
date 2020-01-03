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

import { reducer, initialState } from './pipelineReducer';
import { useApi, useApp, useWorkspace } from 'context';
import { createActions } from './pipelineActions';
import { initializeRoutine } from './pipelineRoutines';

const PipelineStateContext = React.createContext();
const PipelineDispatchContext = React.createContext();

const PipelineProvider = ({ children }) => {
  const [state, dispatch] = React.useReducer(reducer, initialState);
  const { pipelineApi } = useApi();
  const { workspaceName } = useApp();

  React.useEffect(() => {
    if (!pipelineApi) return;
    const actions = createActions({ state, dispatch, pipelineApi });
    actions.fetchPipelines();
  }, [state, pipelineApi]);

  // Reset pipeline state on workspace change
  React.useEffect(() => {
    dispatch(initializeRoutine.trigger());
  }, [workspaceName]);

  return (
    <PipelineStateContext.Provider value={state}>
      <PipelineDispatchContext.Provider value={dispatch}>
        {children}
      </PipelineDispatchContext.Provider>
    </PipelineStateContext.Provider>
  );
};

const usePipelineState = () => {
  const context = React.useContext(PipelineStateContext);
  if (context === undefined) {
    throw new Error('usePipelineState must be used within a PipelineProvider');
  }
  return context;
};

const usePipelineDispatch = () => {
  const context = React.useContext(PipelineDispatchContext);
  if (context === undefined) {
    throw new Error(
      'usePipelineDispatch must be used within a PipelineProvider',
    );
  }
  return context;
};

PipelineProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

const usePipelineActions = () => {
  const state = usePipelineState();
  const dispatch = usePipelineDispatch();
  const { pipelineApi } = useApi();
  const { groups } = useApp();
  const { currentPipeline } = useWorkspace();
  return createActions({
    state,
    dispatch,
    pipelineApi,
    currentPipeline,
    ...groups,
  });
};

export {
  PipelineProvider,
  usePipelineState,
  usePipelineDispatch,
  usePipelineActions,
};
