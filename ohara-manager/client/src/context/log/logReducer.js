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

import {
  initializeRoutine,
  fetchConfiguratorRoutine,
  fetchZookeeperRoutine,
  fetchBrokerRoutine,
  fetchWorkerRoutine,
  fetchStreamRoutine,
} from './logRoutines';

const initialState = {
  isFetching: false,
  data: {},
  lastUpdated: null,
  error: null,
};

const reducer = (state, action) => {
  switch (action.type) {
    case initializeRoutine.TRIGGER:
      return initialState;
    case fetchConfiguratorRoutine.REQUEST:
    case fetchZookeeperRoutine.REQUEST:
    case fetchBrokerRoutine.REQUEST:
    case fetchWorkerRoutine.REQUEST:
    case fetchStreamRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
      };
    case fetchConfiguratorRoutine.SUCCESS:
    case fetchZookeeperRoutine.SUCCESS:
    case fetchBrokerRoutine.SUCCESS:
    case fetchWorkerRoutine.SUCCESS:
    case fetchStreamRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: action.payload,
        lastUpdated: new Date(),
      };
    case fetchConfiguratorRoutine.FAILURE:
    case fetchZookeeperRoutine.FAILURE:
    case fetchBrokerRoutine.FAILURE:
    case fetchWorkerRoutine.FAILURE:
    case fetchStreamRoutine.FAILURE:
      return {
        ...state,
        isFetching: false,
        error: action.payload,
      };

    default:
      return state;
  }
};

export { reducer, initialState };
