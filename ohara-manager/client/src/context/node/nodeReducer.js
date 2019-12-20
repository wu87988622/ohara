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
  fetchNodesRoutine,
  addNodeRoutine,
  updateNodeRoutine,
  deleteNodeRoutine,
} from './nodeRoutines';

const initialState = {
  data: [],
  isFetching: false,
  lastUpdated: null,
  error: null,
};

const sort = nodes => sortBy(nodes, 'settings.name');

const isEqual = (object, other) =>
  isEqualWith(object, other, ['settings.name', 'settings.group']);

const reducer = (state, action) => {
  switch (action.type) {
    case fetchNodesRoutine.REQUEST:
    case addNodeRoutine.REQUEST:
    case updateNodeRoutine.REQUEST:
    case deleteNodeRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
      };
    case fetchNodesRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sort(action.payload),
        lastUpdated: new Date(),
      };
    case addNodeRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sort([...state.data, action.payload]),
        lastUpdated: new Date(),
      };
    case updateNodeRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: map(state.data, node =>
          isEqual(node, action.payload) ? action.payload : node,
        ),
        lastUpdated: new Date(),
      };
    case deleteNodeRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: reject(state.data, node => {
          return node.hostname === action.payload.hostname;
        }),
        lastUpdated: new Date(),
      };
    case fetchNodesRoutine.FAILURE:
    case addNodeRoutine.FAILURE:
    case updateNodeRoutine.FAILURE:
    case deleteNodeRoutine.FAILURE:
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
