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

import { map, reject } from 'lodash';
import { isKeyEqual, sortByName } from 'utils/object';
import {
  fetchBrokersRoutine,
  createBrokerRoutine,
  updateBrokerRoutine,
  stageBrokerRoutine,
  deleteBrokerRoutine,
  startBrokerRoutine,
  stopBrokerRoutine,
} from './brokerRoutines';

const initialState = {
  data: [],
  isFetching: false,
  lastUpdated: null,
  error: null,
  stagingSettings: [],
};

const reducer = (state, action) => {
  switch (action.type) {
    case fetchBrokersRoutine.REQUEST:
    case createBrokerRoutine.REQUEST:
    case updateBrokerRoutine.REQUEST:
    case stageBrokerRoutine.REQUEST:
    case deleteBrokerRoutine.REQUEST:
    case startBrokerRoutine.REQUEST:
    case stopBrokerRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
        error: null,
      };
    case fetchBrokersRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sortByName(action.payload),
        lastUpdated: new Date(),
      };
    case createBrokerRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sortByName([...state.data, action.payload]),
        lastUpdated: new Date(),
      };
    case updateBrokerRoutine.SUCCESS:
    case stageBrokerRoutine.SUCCESS:
    case startBrokerRoutine.SUCCESS:
    case stopBrokerRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: map(state.data, broker =>
          isKeyEqual(broker, action.payload)
            ? { ...broker, ...action.payload }
            : broker,
        ),
        lastUpdated: new Date(),
      };
    case deleteBrokerRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: reject(state.data, broker => isKeyEqual(broker, action.payload)),
        lastUpdated: new Date(),
      };
    case fetchBrokersRoutine.FAILURE:
    case createBrokerRoutine.FAILURE:
    case updateBrokerRoutine.FAILURE:
    case stageBrokerRoutine.FAILURE:
    case deleteBrokerRoutine.FAILURE:
    case startBrokerRoutine.FAILURE:
    case stopBrokerRoutine.FAILURE:
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
