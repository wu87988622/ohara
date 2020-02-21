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
import * as routines from './streamRoutines';

const initialState = {
  data: [],
  isFetching: false,
  lastUpdated: null,
  error: null,
};

const reducer = (state, action) => {
  switch (action.type) {
    case routines.initializeRoutine.TRIGGER:
      return initialState;

    case routines.fetchStreamsRoutine.REQUEST:
    case routines.createStreamRoutine.REQUEST:
    case routines.updateStreamRoutine.REQUEST:
    case routines.deleteStreamRoutine.REQUEST:
    case routines.startStreamRoutine.REQUEST:
    case routines.stopStreamRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
        error: null,
      };
    case routines.fetchStreamsRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sortByName(action.payload),
        lastUpdated: new Date(),
      };
    case routines.createStreamRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sortByName([...state.data, action.payload]),
        lastUpdated: new Date(),
      };
    case routines.startStreamRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sortByName([...state.data, action.payload]),
        lastUpdated: new Date(),
      };
    case routines.stopStreamRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sortByName([...state.data, action.payload]),
        lastUpdated: new Date(),
      };
    case routines.updateStreamRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: map(state.data, stream =>
          isKeyEqual(stream, action.payload)
            ? { ...stream, ...action.payload }
            : stream,
        ),
        lastUpdated: new Date(),
      };
    case routines.deleteStreamRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: reject(state.data, stream => isKeyEqual(stream, action.payload)),
        lastUpdated: new Date(),
      };
    case routines.fetchStreamsRoutine.FAILURE:
    case routines.createStreamRoutine.FAILURE:
    case routines.updateStreamRoutine.FAILURE:
    case routines.deleteStreamRoutine.FAILURE:
    case routines.startStreamRoutine.FAILURE:
    case routines.stopStreamRoutine.FAILURE:
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
