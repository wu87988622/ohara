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

import { reject, includes } from 'lodash';
import * as actions from 'store/actions';
import { ENTITY_TYPE } from 'store/schema';
import { entity } from './index';

const initialState = {
  data: [],
  settings: {
    data: {
      limit: 1000,
      unlimited: false,
    },
  },
  notifications: {
    data: {
      warning: 0,
      error: 0,
    },
  },
};

export default function reducer(state = initialState, action) {
  switch (action.type) {
    case actions.fetchEventLogs.TRIGGER:
      return {
        ...state,
        data: [],
      };
    case actions.fetchEventLogs.SUCCESS:
      return {
        ...state,
        data: action.payload || [],
      };
    case actions.createEventLog.SUCCESS:
      return {
        ...state,
        data: [...state.data, action.payload],
      };
    case actions.deleteEventLogs.SUCCESS:
      return {
        ...state,
        data: reject(state.data, log => {
          return includes(action.payload, log.key);
        }),
      };
    case actions.clearEventLogs.SUCCESS:
      return {
        ...state,
        data: [],
      };
    case actions.updateSettings.SUCCESS:
      return {
        ...state,
        settings: {
          ...state.settings,
          data: { ...state.settings.data, ...action.payload },
        },
      };
    case actions.updateNotifications.SUCCESS:
      return {
        ...state,
        notifications: {
          ...state.notifications,
          data: { ...state.notifications.data, ...action.payload },
        },
      };
    case actions.clearNotifications.SUCCESS:
      return {
        ...state,
        notifications: {
          ...state.notifications,
          data: { ...initialState.notifications.data },
        },
      };
    default:
      return entity(ENTITY_TYPE.eventLogs)(state, action);
  }
}
