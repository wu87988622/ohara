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

import * as routine from './topicDataRoutines';

const initialState = {
  isFetching: false,
  query: {
    name: '',
    limit: 10,
  },
  data: {},
  lastUpdated: null,
  error: null,
};

const reducer = (state, action) => {
  switch (action.type) {
    case routine.initializeRoutine.TRIGGER:
      return initialState;
    case routine.refetchTopicRoutine.TRIGGER:
      return {
        ...state,
        lastUpdated: null,
      };
    case routine.setNameRoutine.REQUEST:
    case routine.setLimitRoutine.REQUEST:
      return {
        ...state,
        query: Object.assign({}, state.query, action.payload),
      };
    case routine.fetchTopicDataRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
      };
    case routine.fetchTopicDataRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        lastUpdated: new Date(),
        data: action.payload,
      };
    case routine.fetchTopicDataRoutine.FAILURE:
      return {
        ...state,
        isFetching: false,
        lastUpdated: new Date(),
        error: action.payload,
      };

    default:
      return state;
  }
};

export { reducer, initialState };
