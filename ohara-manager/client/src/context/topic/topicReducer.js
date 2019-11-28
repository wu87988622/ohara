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

import { reject, get } from 'lodash';

import { changeWorkspaceRoutine } from 'context/workspace/workspaceRoutines';
import {
  fetchTopicsRoutine,
  addTopicRoutine,
  deleteTopicRoutine,
} from './topicRoutines';

const initialState = {
  workspace: null,
  isFetching: false,
  data: [],
  lastUpdated: null,
  error: null,
};

const sortedTopics = topics =>
  topics.sort((a, b) => a.settings.name.localeCompare(b.settings.name));

const reducer = (state, action) => {
  switch (action.type) {
    case fetchTopicsRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
      };
    case fetchTopicsRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: action.payload,
        lastUpdated: new Date(),
      };
    case fetchTopicsRoutine.FAILURE:
      return {
        ...state,
        isFetching: false,
        error: action.payload,
      };
    case addTopicRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
        error: undefined,
      };
    case addTopicRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sortedTopics([...state.data, action.payload]),
        lastUpdated: new Date(),
      };
    case addTopicRoutine.FAILURE:
      return {
        ...state,
        isFetching: false,
        error: action.payload,
      };
    case deleteTopicRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
      };
    case deleteTopicRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: reject(state.data, topic => {
          return (
            topic.settings.name === action.payload.name &&
            topic.settings.group === action.payload.group
          );
        }),
        lastUpdated: new Date(),
      };
    case deleteTopicRoutine.FAILURE:
      return {
        ...state,
        isFetching: false,
        error: action.payload,
      };
    case changeWorkspaceRoutine.TRIGGER: {
      if (
        get(state.workspace, 'settings.name') !==
        get(action.payload, 'settings.name')
      ) {
        return { ...initialState, workspace: action.payload };
      } else {
        return state;
      }
    }

    default:
      return state;
  }
};

export { reducer, initialState };
