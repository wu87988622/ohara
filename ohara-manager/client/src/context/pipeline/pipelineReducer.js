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

import _ from 'lodash';

import * as routines from './pipelineRoutines';
import { isKeyEqual } from 'utils/object';

const sort = pipelines => _.sortBy(pipelines, 'name');

// The objects filed returned by the pipelines API now contains objects
// which are not supposed to be appeared. Filtering out these objects
// in the frontend for now
const filterOutObjects = pipelines =>
  pipelines.map(pipeline => {
    return {
      ...pipeline,
      objects: pipeline.objects.filter(object => object.kind !== 'object'),
    };
  });

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
        data: sort(filterOutObjects(action.payload)),
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
      const newData = state.data.map(pipeline =>
        isKeyEqual(pipeline, action.payload)
          ? { ...pipeline, ...action.payload }
          : pipeline,
      );

      return {
        ...state,
        isFetching: false,
        data: filterOutObjects(newData),
        lastUpdated: new Date(),
      };
    case routines.deletePipelineRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: state.data.filter(
          pipeline => !isKeyEqual(pipeline, action.payload),
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
