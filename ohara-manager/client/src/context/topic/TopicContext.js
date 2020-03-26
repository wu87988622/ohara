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

import { initializeRoutine } from './topicRoutines';
import { useApi } from 'context';
import * as hooks from 'hooks';
import { createActions } from './topicActions';
import { reducer, initialState } from './topicReducer';

const TopicStateContext = React.createContext();
const TopicDispatchContext = React.createContext();

const TopicProvider = ({ children }) => {
  const [state, dispatch] = React.useReducer(reducer, initialState);
  const eventLog = hooks.useEventLog();
  const workspaceName = hooks.useWorkspaceName();
  const { topicApi } = useApi();

  React.useEffect(() => {
    dispatch(initializeRoutine.trigger());
  }, [workspaceName]);

  React.useEffect(() => {
    if (!topicApi) return;
    const actions = createActions({ state, dispatch, eventLog, topicApi });
    actions.fetchTopics();
  }, [state, dispatch, eventLog, topicApi]);

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
  const eventLog = hooks.useEventLog();
  const { topicApi } = useApi();
  return React.useMemo(
    () => createActions({ state, dispatch, eventLog, topicApi }),
    [state, dispatch, eventLog, topicApi],
  );
};

export { TopicProvider, useTopicState, useTopicDispatch, useTopicActions };
