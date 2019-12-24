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
  fetchStreamsCreator,
  addStreamCreator,
  updateStreamCreator,
  deleteStreamCreator,
} from './streamActions';
import { reducer, initialState } from './streamReducer';

const StreamStateContext = React.createContext();
const StreamDispatchContext = React.createContext();

const StreamProvider = ({ children }) => {
  const [state, dispatch] = React.useReducer(reducer, initialState);
  const showMessage = useSnackbar();

  const fetchStreams = React.useCallback(
    fetchStreamsCreator(state, dispatch, showMessage),
    [state],
  );

  React.useEffect(() => {
    fetchStreams();
  }, [fetchStreams]);

  return (
    <StreamStateContext.Provider value={state}>
      <StreamDispatchContext.Provider value={dispatch}>
        {children}
      </StreamDispatchContext.Provider>
    </StreamStateContext.Provider>
  );
};

StreamProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

const useStreamState = () => {
  const context = React.useContext(StreamStateContext);
  if (context === undefined) {
    throw new Error('useStreamState must be used within a StreamProvider');
  }
  return context;
};

const useStreamDispatch = () => {
  const context = React.useContext(StreamDispatchContext);
  if (context === undefined) {
    throw new Error('useStreamDispatch must be used within a StreamProvider');
  }
  return context;
};

const useStreamActions = () => {
  const state = useStreamState();
  const dispatch = useStreamDispatch();
  const showMessage = useSnackbar();
  return {
    addStream: addStreamCreator(state, dispatch, showMessage),
    updateStream: updateStreamCreator(state, dispatch, showMessage),
    deleteStream: deleteStreamCreator(state, dispatch, showMessage),
  };
};

export { StreamProvider, useStreamState, useStreamDispatch, useStreamActions };
