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

import { map, reject, sortBy, isEqualWith } from 'lodash';

import {
  fetchBrokersRoutine,
  addBrokerRoutine,
  updateBrokerRoutine,
  deleteBrokerRoutine,
} from './brokerRoutines';

const initialState = {
  data: [],
  isFetching: false,
  lastUpdated: null,
  error: null,
};

const sort = brokers => sortBy(brokers, 'settings.name');

const isEqual = (object, other) =>
  isEqualWith(object, other, ['settings.name', 'settings.group']);

const reducer = (state, action) => {
  switch (action.type) {
    case fetchBrokersRoutine.REQUEST:
    case addBrokerRoutine.REQUEST:
    case updateBrokerRoutine.REQUEST:
    case deleteBrokerRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
      };
    case fetchBrokersRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sort(action.payload),
        lastUpdated: new Date(),
      };
    case addBrokerRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sort([...state.data, action.payload]),
        lastUpdated: new Date(),
      };
    case updateBrokerRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: map(state.data, broker =>
          isEqual(broker, action.payload) ? action.payload : broker,
        ),
        lastUpdated: new Date(),
      };
    case deleteBrokerRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: reject(state.data, broker => {
          return (
            broker.settings.name === action.payload.name &&
            broker.settings.group === action.payload.group
          );
        }),
        lastUpdated: new Date(),
      };
    case fetchBrokersRoutine.FAILURE:
    case addBrokerRoutine.FAILURE:
    case updateBrokerRoutine.FAILURE:
    case deleteBrokerRoutine.FAILURE:
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
