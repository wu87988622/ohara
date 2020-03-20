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

import * as context from 'context';
import * as hooks from 'hooks';
import { useEventLog } from 'context/eventLog/eventLogHooks';
import { initializeRoutine } from './topicDataRoutines';
import { createActions } from './topicDataActions';
import { reducer, initialState } from './topicDataReducer';

const TopicDataStateContext = React.createContext();
const TopicDataDispatchContext = React.createContext();

const TopicDataProvider = ({ children }) => {
  const [state, dispatch] = React.useReducer(reducer, initialState);
  const workspaceName = hooks.useWorkspaceName();

  React.useEffect(() => {
    dispatch(initializeRoutine.trigger());
  }, [workspaceName]);

  return (
    <TopicDataStateContext.Provider value={state}>
      <TopicDataDispatchContext.Provider value={dispatch}>
        {children}
      </TopicDataDispatchContext.Provider>
    </TopicDataStateContext.Provider>
  );
};

const useTopicDataState = () => {
  const context = React.useContext(TopicDataStateContext);

  if (context === undefined) {
    throw new Error(
      'useTopicDataState must be used within a TopicDataProvider',
    );
  }
  return context;
};

const useTopicDataDispatch = () => {
  const context = React.useContext(TopicDataDispatchContext);
  if (context === undefined) {
    throw new Error(
      'useTopicDataDispatch must be used within a TopicDataProvider',
    );
  }
  return context;
};

TopicDataProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

const useTopicDataActions = () => {
  const state = useTopicDataState();
  const dispatch = useTopicDataDispatch();
  const eventLog = useEventLog();
  const { topicApi } = context.useApi();
  return React.useMemo(
    () => createActions({ state, dispatch, eventLog, topicApi }),
    [state, dispatch, eventLog, topicApi],
  );
};

export {
  TopicDataProvider,
  useTopicDataState,
  useTopicDataDispatch,
  useTopicDataActions,
};
