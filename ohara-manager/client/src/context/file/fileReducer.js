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

import { reject, get } from 'lodash';

import { changeWorkspaceRoutine } from 'context/workspace/workspaceRoutines';
import {
  fetchFilesRoutine,
  uploadFileRoutine,
  deleteFileRoutine,
} from './fileRoutines';

const initialState = {
  workspace: null,
  isFetching: false,
  data: [],
  lastUpdated: null,
  error: null,
};

const sortedFiles = Files => Files.sort((a, b) => a.name.localeCompare(b.name));

const reducer = (state, action) => {
  switch (action.type) {
    case fetchFilesRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
      };
    case fetchFilesRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: action.payload,
        lastUpdated: new Date(),
      };
    case fetchFilesRoutine.FAILURE:
      return {
        ...state,
        isFetching: false,
        error: action.payload,
      };
    case uploadFileRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
        error: undefined,
      };
    case uploadFileRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: sortedFiles([...state.data, action.payload]),
        lastUpdated: new Date(),
      };
    case uploadFileRoutine.FAILURE:
      return {
        ...state,
        isFetching: false,
        error: action.payload,
      };
    case deleteFileRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
      };
    case deleteFileRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: reject(state.data, file => {
          return (
            file.name === action.payload.name &&
            file.group === action.payload.group
          );
        }),
        lastUpdated: new Date(),
      };
    case deleteFileRoutine.FAILURE:
      return {
        ...state,
        isFetching: false,
        error: action.payload,
      };
    case changeWorkspaceRoutine.TRIGGER: {
      if (get(state.workspace, 'name') !== get(action.payload, 'name')) {
        return { ...initialState, workspace: action.payload };
      } else {
        return state;
      }
    }
    default:
      return state;
  }
};

export { reducer, initialState };
