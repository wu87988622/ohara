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
  fetchZookeepersRoutine,
  createZookeeperRoutine,
  updateZookeeperRoutine,
  stageZookeeperRoutine,
  deleteZookeeperRoutine,
  startZookeeperRoutine,
  stopZookeeperRoutine,
} from './zookeeperRoutines';

const initialState = {
  data: [],
  isFetching: false,
  lastUpdated: null,
  error: null,
  stagingSettings: [],
};

const reducer = (state, action) => {
  switch (action.type) {
    case fetchZookeepersRoutine.REQUEST:
    case createZookeeperRoutine.REQUEST:
    case updateZookeeperRoutine.REQUEST:
    case stageZookeeperRoutine.REQUEST:
    case deleteZookeeperRoutine.REQUEST:
    case startZookeeperRoutine.REQUEST:
    case stopZookeeperRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
        error: null,
      };
    case fetchZookeepersRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sortByName(action.payload),
        lastUpdated: new Date(),
      };
    case createZookeeperRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sortByName([...state.data, action.payload]),
        lastUpdated: new Date(),
      };
    case updateZookeeperRoutine.SUCCESS:
    case startZookeeperRoutine.SUCCESS:
    case stopZookeeperRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: map(state.data, zookeeper =>
          isEqualByKey(zookeeper, action.payload)
            ? { ...zookeeper, ...omit(action.payload, 'stagingSettings') }
            : zookeeper,
        ),
        lastUpdated: new Date(),
      };
    case stageZookeeperRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: map(state.data, zookeeper =>
          isEqualByKey(zookeeper, action.payload)
            ? { ...zookeeper, stagingSettings: action.payload.stagingSettings }
            : zookeeper,
        ),
        lastUpdated: new Date(),
      };
    case deleteZookeeperRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: reject(state.data, zookeeper =>
          isEqualByKey(zookeeper, action.payload),
        ),
        lastUpdated: new Date(),
      };
    case fetchZookeepersRoutine.FAILURE:
    case createZookeeperRoutine.FAILURE:
    case updateZookeeperRoutine.FAILURE:
    case stageZookeeperRoutine.FAILURE:
    case deleteZookeeperRoutine.FAILURE:
    case startZookeeperRoutine.FAILURE:
    case stopZookeeperRoutine.FAILURE:
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
