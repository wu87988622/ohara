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

import { map, reject, omit } from 'lodash';
import { isKeyEqual, sortByName } from 'utils/object';

import {
  fetchConnectorsRoutine,
  createConnectorRoutine,
  updateConnectorRoutine,
  deleteConnectorRoutine,
  stageConnectorRoutine,
  startConnectorRoutine,
  stopConnectorRoutine,
  initializeRoutine,
} from './connectorRoutines';

const initialState = {
  data: [],
  isFetching: false,
  lastUpdated: null,
  error: null,
};

const reducer = (state, action) => {
  switch (action.type) {
    case initializeRoutine.TRIGGER:
      return initialState;

    case fetchConnectorsRoutine.REQUEST:
    case createConnectorRoutine.REQUEST:
    case updateConnectorRoutine.REQUEST:
    case stageConnectorRoutine.REQUEST:
    case deleteConnectorRoutine.REQUEST:
    case startConnectorRoutine.REQUEST:
    case stopConnectorRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
        error: null,
      };
    case fetchConnectorsRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sortByName(action.payload),
        lastUpdated: new Date(),
      };
    case createConnectorRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sortByName([...state.data, action.payload]),
        lastUpdated: new Date(),
      };
    case updateConnectorRoutine.SUCCESS:
    case stageConnectorRoutine.SUCCESS:
    case startConnectorRoutine.SUCCESS:
    case stopConnectorRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: map(state.data, connector =>
          isKeyEqual(connector, action.payload)
            ? { ...omit(connector, ['state']), ...action.payload }
            : connector,
        ),
        lastUpdated: new Date(),
      };
    case deleteConnectorRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: reject(state.data, connector =>
          isKeyEqual(connector, action.payload),
        ),
        lastUpdated: new Date(),
      };
    case fetchConnectorsRoutine.FAILURE:
    case createConnectorRoutine.FAILURE:
    case updateConnectorRoutine.FAILURE:
    case stageConnectorRoutine.FAILURE:
    case deleteConnectorRoutine.FAILURE:
    case startConnectorRoutine.FAILURE:
    case stopConnectorRoutine.FAILURE:
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
