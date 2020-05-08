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

import { LOG_TIME_GROUP } from 'const';
import * as actions from 'store/actions';

const initialState = {
  topicData: {
    isFetching: false,
    data: [],
    query: {
      name: '',
      limit: 10,
    },
    lastUpdated: null,
    error: null,
  },
  log: {
    isFetching: false,
    data: [],
    query: {
      logType: '',
      hostName: '',
      shabondiKey: {},
      streamKey: {},
      timeGroup: LOG_TIME_GROUP.latest,
      timeRange: 10,
      startTime: '',
      endTime: '',
    },
    lastUpdated: null,
    error: null,
  },
};

export default function reducer(state = initialState, action) {
  switch (action.type) {
    case actions.fetchDevToolTopicData.TRIGGER:
      return {
        ...state,
        topicData: {
          ...state.topicData,
          isFetching: true,
        },
      };
    case actions.fetchDevToolTopicData.SUCCESS:
      return {
        ...state,
        topicData: {
          ...state.topicData,
          isFetching: false,
          data: action.payload || {},
          lastUpdated: new Date(),
          error: null,
        },
      };
    case actions.setDevToolTopicQueryParams.SUCCESS:
      return {
        ...state,
        topicData: {
          ...state.topicData,
          isFetching: false,
          query: Object.assign({}, state.topicData.query, action.payload),
          lastUpdated: new Date(),
          error: null,
        },
      };
    case actions.fetchDevToolLog.TRIGGER:
      return {
        ...state,
        log: {
          ...state.log,
          isFetching: true,
        },
      };
    case actions.fetchDevToolLog.SUCCESS:
      return {
        ...state,
        log: {
          ...state.log,
          isFetching: false,
          data: action.payload || [],
          lastUpdated: new Date(),
          error: null,
        },
      };
    case actions.setDevToolLogQueryParams.SUCCESS:
      return {
        ...state,
        log: {
          ...state.log,
          isFetching: false,
          query: Object.assign({}, state.log.query, action.payload),
          lastUpdated: new Date(),
          error: null,
        },
      };
    case actions.fetchDevToolTopicData.FAILURE:
    case actions.setDevToolTopicQueryParams.FAILURE:
      return {
        ...state,
        topicData: {
          ...state.topicData,
          isFetching: false,
          error: action.payload,
        },
      };
    case actions.fetchDevToolLog.FAILURE:
    case actions.setDevToolLogQueryParams.FAILURE:
      return {
        ...state,
        log: {
          ...state.log,
          isFetching: false,
          error: action.payload,
        },
      };

    default:
      return state;
  }
}
