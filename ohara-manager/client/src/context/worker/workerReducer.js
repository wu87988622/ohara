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

import { map, omit, reject } from 'lodash';
import { isEqualByKey, sortByName } from 'utils/object';
import {
  fetchWorkersRoutine,
  createWorkerRoutine,
  updateWorkerRoutine,
  stageWorkerRoutine,
  deleteWorkerRoutine,
  startWorkerRoutine,
  stopWorkerRoutine,
} from './workerRoutines';

const initialState = {
  data: [],
  isFetching: false,
  lastUpdated: null,
  error: null,
  stagingSettings: [],
};

const reducer = (state, action) => {
  switch (action.type) {
    case fetchWorkersRoutine.REQUEST:
    case createWorkerRoutine.REQUEST:
    case updateWorkerRoutine.REQUEST:
    case stageWorkerRoutine.REQUEST:
    case deleteWorkerRoutine.REQUEST:
    case startWorkerRoutine.REQUEST:
    case stopWorkerRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
        error: null,
      };
    case fetchWorkersRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sortByName(action.payload),
        lastUpdated: new Date(),
      };
    case createWorkerRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sortByName([...state.data, action.payload]),
        lastUpdated: new Date(),
      };
    case updateWorkerRoutine.SUCCESS:
    case startWorkerRoutine.SUCCESS:
    case stopWorkerRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: map(state.data, worker =>
          isEqualByKey(worker, action.payload)
            ? { ...worker, ...omit(action.payload, 'stagingSettings') }
            : worker,
        ),
        lastUpdated: new Date(),
      };
    case stageWorkerRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: map(state.data, worker =>
          isEqualByKey(worker, action.payload)
            ? { ...worker, stagingSettings: action.payload.stagingSettings }
            : worker,
        ),
        lastUpdated: new Date(),
      };
    case deleteWorkerRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: reject(state.data, worker =>
          isEqualByKey(worker, action.payload),
        ),
        lastUpdated: new Date(),
      };
    case fetchWorkersRoutine.FAILURE:
    case createWorkerRoutine.FAILURE:
    case updateWorkerRoutine.FAILURE:
    case stageWorkerRoutine.FAILURE:
    case deleteWorkerRoutine.FAILURE:
    case startWorkerRoutine.FAILURE:
    case stopWorkerRoutine.FAILURE:
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
