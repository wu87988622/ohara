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
  fetchStreamsRoutine,
  addStreamRoutine,
  updateStreamRoutine,
  deleteStreamRoutine,
} from './streamRoutines';

const initialState = {
  data: [],
  isFetching: false,
  lastUpdated: null,
  error: null,
};

const sort = streams => sortBy(streams, 'settings.name');

const isEqual = (object, other) =>
  isEqualWith(object, other, ['settings.name', 'settings.group']);

const reducer = (state, action) => {
  switch (action.type) {
    case fetchStreamsRoutine.REQUEST:
    case addStreamRoutine.REQUEST:
    case updateStreamRoutine.REQUEST:
    case deleteStreamRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
      };
    case fetchStreamsRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sort(action.payload),
        lastUpdated: new Date(),
      };
    case addStreamRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sort([...state.data, action.payload]),
        lastUpdated: new Date(),
      };
    case updateStreamRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: map(state.data, stream =>
          isEqual(stream, action.payload) ? action.payload : stream,
        ),
        lastUpdated: new Date(),
      };
    case deleteStreamRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: reject(state.data, stream => {
          return stream.name === action.payload.name;
        }),
        lastUpdated: new Date(),
      };
    case fetchStreamsRoutine.FAILURE:
    case addStreamRoutine.FAILURE:
    case updateStreamRoutine.FAILURE:
    case deleteStreamRoutine.FAILURE:
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
