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

import { map, sortBy, reject, isEqual } from 'lodash';

import * as routines from './pipelineRoutines';

const sort = pipelines => sortBy(pipelines, 'name');

const initialState = {
  data: [],
  selectedCell: null,
  isFetching: false,
  lastUpdated: null,
  error: null,
};

const reducer = (state, action) => {
  switch (action.type) {
    case routines.initializeRoutine.TRIGGER:
      return initialState;
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
    case routines.fetchPipelinesRoutine.REQUEST:
    case routines.createPipelineRoutine.REQUEST:
    case routines.updatePipelineRoutine.REQUEST:
    case routines.deletePipelineRoutine.REQUEST:
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
    case routines.createPipelineRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sort([...state.data, action.payload]),
        lastUpdated: new Date(),
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
    case routines.deletePipelineRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: reject(
          state.data,
          pipeline =>
            pipeline.name === action.payload.name ||
            pipeline.group === action.payload.group,
        ),
        lastUpdated: new Date(),
      };
    case routines.fetchPipelinesRoutine.FAILURE:
    case routines.createPipelineRoutine.FAILURE:
    case routines.deletePipelineRoutine.FAILURE:
    case routines.updatePipelineRoutine.FAILURE:
      return {
        ...state,
        isFetching: false,
        error: action.payload || true,
      };

    default:
      return state;
  }
};

export { reducer, initialState };
