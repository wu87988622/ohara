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

import { sortBy, map, isEqual } from 'lodash';

import * as routines from './pipelineRoutines';

const sort = pipelines => sortBy(pipelines, 'name');

const initialState = {
  data: [],
  currentPipeline: null,
  selectedCell: null,
  isFetching: false,
  lastUpdated: null,
  error: null,
};

const reducer = (state, action) => {
  switch (action.type) {
    case routines.fetchPipelinesRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
      };

    case routines.fetchPipelinesRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sort(action.payload),
        lastUpdated: new Date(),
      };

    case routines.fetchPipelinesRoutine.FAILURE:
      return {
        ...state,
        isFetching: false,
        error: action.payload || true,
      };

    case routines.addPipelineRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
        error: null,
      };

    case routines.addPipelineRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sort([...state.data, action.payload]),
        lastUpdated: new Date(),
      };

    case routines.addPipelineRoutine.FAILURE:
      return {
        ...state,
        isFetching: false,
        error: action.payload || true,
      };

    case routines.deletePipelineRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
      };

    case routines.deletePipelineRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: state.data.filter(
          pipeline =>
            pipeline.name !== action.payload.name &&
            pipeline.group !== action.payload.name,
        ),
        lastUpdated: new Date(),
      };

    case routines.deletePipelineRoutine.FAILURE:
      return {
        ...state,
        isFetching: false,
        error: action.payload || true,
      };

    case routines.updatePipelineRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
      };

    case routines.updatePipelineRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: map(state.data, pipeline =>
          isEqual(pipeline, action.payload) ? action.payload : pipeline,
        ),
        lastUpdated: new Date(),
      };

    case routines.updatePipelineRoutine.FAILURE:
      return {
        ...state,
        isFetching: false,
        error: action.payload || true,
      };

    case routines.setCurrentPipelineRoutine.TRIGGER:
      return {
        ...state,
        currentPipeline:
          state.data.find(data => data.name === action.payload) || null,
      };

    case routines.setSelectedCellRoutine.TRIGGER:
      return {
        ...state,
        selectedCell: action.payload,
      };

    case routines.initializeRoutine.TRIGGER:
      return initialState;

    default:
      return state;
  }
};

export { reducer, initialState };
