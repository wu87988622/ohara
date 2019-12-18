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

import { sortBy } from 'lodash';

import {
  initializeRoutine,
  fetchPipelinesRoutine,
  addPipelineRoutine,
  setCurrentPipelineRoutine,
} from './pipelineRoutines';

const sort = pipelines => sortBy(pipelines, 'name');

const initialState = {
  data: [],
  currentPipeline: null,
  isFetching: false,
  lastUpdated: null,
  error: null,
};

const reducer = (state, action) => {
  switch (action.type) {
    case fetchPipelinesRoutine.REQUEST:
    case addPipelineRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
      };
    case fetchPipelinesRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sort(action.payload),
        lastUpdated: new Date(),
      };
    case addPipelineRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sort([...state.data, action.payload]),
        lastUpdated: new Date(),
      };
    case fetchPipelinesRoutine.FAILURE:
      return {
        ...state,
        isFetching: false,
        error: action.payload || true,
      };
    case setCurrentPipelineRoutine.TRIGGER:
      return {
        ...state,
        currentPipeline:
          state.data.find(data => data.name === action.payload) || null,
      };
    case initializeRoutine.TRIGGER:
      return initialState;
    default:
      return state;
  }
};

export { reducer, initialState };
