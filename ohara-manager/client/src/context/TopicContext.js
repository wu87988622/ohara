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
import { isEmpty } from 'lodash';
import { useSnackbar } from 'context/SnackbarContext';
import { useWorkspace } from 'context/WorkspaceContext';
import {
  createFetchTopics,
  createAddTopic,
  createDeleteTopic,
} from 'actions/topic';
import { reducer, initialState } from 'reducers/topic';
import { changeWorkspaceRoutine } from 'routines/workspace';

const TopicStateContext = React.createContext();
const TopicDispatchContext = React.createContext();

const TopicProvider = ({ children }) => {
  const [state, dispatch] = React.useReducer(reducer, initialState);
  const { workspaces, currentWorkspace } = useWorkspace();

  React.useEffect(() => {
    if (isEmpty(workspaces)) return;
    dispatch(changeWorkspaceRoutine.trigger(currentWorkspace));
  }, [workspaces, currentWorkspace]);

  return (
    <TopicStateContext.Provider value={state}>
      <TopicDispatchContext.Provider value={dispatch}>
        {children}
      </TopicDispatchContext.Provider>
    </TopicStateContext.Provider>
  );
};

const useTopicState = () => {
  const context = React.useContext(TopicStateContext);
  if (context === undefined) {
    throw new Error('useTopicState must be used within a TopicProvider');
  }
  return context;
};

const useTopicDispatch = () => {
  const context = React.useContext(TopicDispatchContext);
  if (context === undefined) {
    throw new Error('useTopicDispatch must be used within a TopicProvider');
  }
  return context;
};

TopicProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

const useTopicActions = () => {
  const state = useTopicState();
  const dispatch = useTopicDispatch();
  const showMessage = useSnackbar();
  return {
    fetchTopics: createFetchTopics(state, dispatch),
    addTopic: createAddTopic(state, dispatch, showMessage),
    deleteTopic: createDeleteTopic(state, dispatch, showMessage),
  };
};

export { TopicProvider, useTopicState, useTopicDispatch, useTopicActions };
