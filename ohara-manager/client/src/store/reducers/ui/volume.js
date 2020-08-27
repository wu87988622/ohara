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

import * as actions from 'store/actions';

const initialState = {
  loading: false,
  lastUpdated: null,
  error: null,
};

function volume(state = initialState, action) {
  switch (action.type) {
    case actions.fetchVolume.REQUEST:
      return {
        ...state,
        loading: true,
      };
    case actions.fetchVolume.SUCCESS:
      return {
        ...state,
        name: action.payload.name,
        loading: false,
        lastUpdated: new Date(),
      };
    case actions.fetchVolume.FAILURE:
      return {
        ...state,
        loading: false,
        error: action.payload,
      };
    case actions.validateVolumePath.REQUEST:
      return {
        validate: { validating: true },
      };
    case actions.validateVolumePath.SUCCESS:
      return {
        validate: {
          validating: false,
          path: state.path,
          isValidate: true,
        },
      };
    case actions.validateVolumePath.FAILURE:
      return {
        ...state,
        validate: {
          validating: false,
          path: state.path,
          isValidate: false,
          ...action.payload,
        },
      };
    default:
      return state;
  }
}

export default function reducer(state = {}, action) {
  if (action.payload?.volumeId) {
    return {
      ...state,
      [action.payload.volumeId]: volume(state[action.payload.volumeId], action),
    };
  }
  return state;
}
