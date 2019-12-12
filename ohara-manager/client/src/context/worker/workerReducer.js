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

import { map, reject, sortBy } from 'lodash';

import {
  fetchWorkersRoutine,
  addWorkerRoutine,
  updateWorkerRoutine,
  deleteWorkerRoutine,
  updateStagingSettingsRoutine,
} from './workerRoutines';

const initialState = {
  data: [],
  isFetching: false,
  lastUpdated: null,
  error: null,
  stagingSettings: [],
};

const sort = workers => sortBy(workers, 'settings.name');

const reducer = (state, action) => {
  switch (action.type) {
    case fetchWorkersRoutine.REQUEST:
    case addWorkerRoutine.REQUEST:
    case updateWorkerRoutine.REQUEST:
    case deleteWorkerRoutine.REQUEST:
    case updateStagingSettingsRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
      };
    case fetchWorkersRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sort(action.payload),
        lastUpdated: new Date(),
      };
    case addWorkerRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sort([...state.data, action.payload]),
        lastUpdated: new Date(),
      };
    case updateWorkerRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: map(state.data, worker => {
          if (
            worker.settings.name === action.payload.name &&
            worker.settings.group === action.payload.group
          ) {
            return action.payload;
          } else {
            return worker;
          }
        }),
        lastUpdated: new Date(),
      };
    case deleteWorkerRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: reject(state.data, worker => {
          return (
            worker.settings.name === action.payload.name &&
            worker.settings.group === action.payload.group
          );
        }),
        lastUpdated: new Date(),
      };
    case updateStagingSettingsRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: map(state.data, worker => {
          if (
            worker.settings.name === action.payload.name &&
            worker.settings.group === action.payload.group
          ) {
            return { ...worker, stagingSettings: action.payload };
          } else {
            return worker;
          }
        }),
        lastUpdated: new Date(),
      };
    case fetchWorkersRoutine.FAILURE:
    case addWorkerRoutine.FAILURE:
    case updateWorkerRoutine.FAILURE:
    case deleteWorkerRoutine.FAILURE:
    case updateStagingSettingsRoutine.FAILURE:
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
