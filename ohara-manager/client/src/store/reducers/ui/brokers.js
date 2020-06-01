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
import { normalize } from 'normalizr';
import * as schema from 'store/schema';
import { getId } from 'utils/object';

const initialState = {
  loading: false,
  lastUpdated: null,
  error: null,
};

function broker(state = initialState, action) {
  switch (action.type) {
    case actions.fetchBroker.REQUEST:
      return {
        ...state,
        loading: true,
      };
    case actions.fetchBroker.SUCCESS:
      return {
        ...state,
        name: action.payload.name,
        loading: false,
        lastUpdated: new Date(),
      };
    case actions.fetchBroker.FAILURE:
      return {
        ...state,
        loading: false,
        error: action.payload,
      };
    case actions.createBroker.SUCCESS:
      return {
        ...normalize(action.payload, schema.broker),
        brokerId: getId(action.payload),
      };
    case actions.createBroker.REQUEST:
      return {
        brokerId: getId(action.payload),
      };
    case actions.createBroker.FAILURE:
      return {
        ...action.payload,
        brokerId: getId(action.payload),
      };
    default:
      return state;
  }
}

export default function reducer(state = {}, action) {
  if (action.payload?.brokerId) {
    return {
      ...state,
      [action.payload.brokerId]: broker(state[action.payload.brokerId], action),
    };
  }
  return state;
}
