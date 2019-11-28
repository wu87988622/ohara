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
  fetchZookeepersRoutine,
  addZookeeperRoutine,
  updateZookeeperRoutine,
  deleteZookeeperRoutine,
} from './zookeeperRoutines';

const initialState = {
  data: [],
  isFetching: false,
  lastUpdated: null,
  error: null,
};

const sort = zookeepers => sortBy(zookeepers, 'settings.name');

const isEqual = (object, other) =>
  isEqualWith(object, other, ['settings.name', 'settings.group']);

const reducer = (state, action) => {
  switch (action.type) {
    case fetchZookeepersRoutine.REQUEST:
    case addZookeeperRoutine.REQUEST:
    case updateZookeeperRoutine.REQUEST:
    case deleteZookeeperRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
      };
    case fetchZookeepersRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sort(action.payload),
        lastUpdated: new Date(),
      };
    case addZookeeperRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sort([...state.data, action.payload]),
        lastUpdated: new Date(),
      };
    case updateZookeeperRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: map(state.data, zookeeper =>
          isEqual(zookeeper, action.payload) ? action.payload : zookeeper,
        ),
        lastUpdated: new Date(),
      };
    case deleteZookeeperRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: reject(state.data, zookeeper => {
          return (
            zookeeper.settings.name === action.payload.name &&
            zookeeper.settings.group === action.payload.group
          );
        }),
        lastUpdated: new Date(),
      };
    case fetchZookeepersRoutine.FAILURE:
    case addZookeeperRoutine.FAILURE:
    case updateZookeeperRoutine.FAILURE:
    case deleteZookeeperRoutine.FAILURE:
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
