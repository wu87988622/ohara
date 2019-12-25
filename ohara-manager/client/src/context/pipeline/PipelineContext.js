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

import * as actions from './pipelineActions';
import { useSnackbar } from 'context/SnackbarContext';
import { initializeRoutine } from './pipelineRoutines';
import { reducer, initialState } from './pipelineReducer';
import { useApp } from 'context';
import { usePrevious } from 'utils/hooks';

const PipelineStateContext = React.createContext();
const PipelineDispatchContext = React.createContext();

const PipelineProvider = ({ children }) => {
  const [state, dispatch] = React.useReducer(reducer, initialState);
  const { data: pipelines, currentPipeline } = state;
  const { workspaceName, pipelineName } = useApp();

  React.useEffect(() => {
    dispatch(initializeRoutine.trigger());
  }, [workspaceName]);

  const prevCurrentPipeline = usePrevious(currentPipeline);
  const setCurrentPipeline = actions.createSetCurrentPipeline(dispatch);
  React.useEffect(() => {
    if (isEmpty(pipelines) || !pipelineName) return;

    const targetPipeline = pipelines.find(
      pipeline => pipeline.name === pipelineName,
    );

    if (!isEqual(targetPipeline, prevCurrentPipeline)) {
      setCurrentPipeline(pipelineName);
    }
  }, [pipelineName, pipelines, prevCurrentPipeline, setCurrentPipeline]);

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

const usePipelineActions = () => {
  const state = usePipelineState();
  const dispatch = usePipelineDispatch();
  const showMessage = useSnackbar();

  return {
    fetchPipelines: actions.createFetchPipelines(state, dispatch, showMessage),
    addPipeline: actions.createAddPipeline(state, dispatch, showMessage),
    deletePipeline: actions.createDeletePipeline(state, dispatch, showMessage),
    updatePipeline: actions.createUpdatePipeline(state, dispatch, showMessage),
    setCurrentPipeline: actions.createSetCurrentPipeline(dispatch),
    setSelectedCell: actions.createSetSelectedCell(dispatch),
  };
};

PipelineProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export {
  PipelineProvider,
  usePipelineState,
  usePipelineDispatch,
  usePipelineActions,
};
