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
  fetchWorkspacesRoutine,
  createWorkspaceRoutine,
  updateWorkspaceRoutine,
  stageWorkspaceRoutine,
  deleteWorkspaceRoutine,
} from './workspaceRoutines';

const initialState = {
  data: [],
  isFetching: false,
  lastUpdated: null,
  error: null,
};

const reducer = (state, action) => {
  switch (action.type) {
    case fetchWorkspacesRoutine.REQUEST:
    case createWorkspaceRoutine.REQUEST:
    case updateWorkspaceRoutine.REQUEST:
    case stageWorkspaceRoutine.REQUEST:
    case deleteWorkspaceRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
        error: null,
      };
    case fetchWorkspacesRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sortByName(action.payload),
        lastUpdated: new Date(),
      };
    case createWorkspaceRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sortByName([...state.data, action.payload]),
        lastUpdated: new Date(),
      };
    case updateWorkspaceRoutine.SUCCESS:
    case stageWorkspaceRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: map(state.data, workspace =>
          isKeyEqual(workspace, action.payload)
            ? { ...workspace, ...action.payload }
            : workspace,
        ),
        lastUpdated: new Date(),
      };
    case deleteWorkspaceRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: reject(state.data, workspace =>
          isKeyEqual(workspace, action.payload),
        ),
        lastUpdated: new Date(),
      };
    case fetchWorkspacesRoutine.FAILURE:
    case createWorkspaceRoutine.FAILURE:
    case updateWorkspaceRoutine.FAILURE:
    case stageWorkspaceRoutine.FAILURE:
    case deleteWorkspaceRoutine.FAILURE:
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
