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
import * as routines from './eventLogRoutines';

const initialState = {
  isFetching: false,
  data: [],
  lastUpdated: null,
  error: null,
  settings: {
    data: {
      limit: 1000,
      unlimited: false,
    },
    isFetching: false,
    lastUpdated: null,
    error: null,
  },
  notifications: {
    data: {
      info: 0,
      error: 0,
    },
    isFetching: false,
    lastUpdated: null,
    error: null,
  },
};

const reducer = (state, action) => {
  switch (action.type) {
    case routines.fetchEventLogsRoutine.REQUEST:
    case routines.createEventLogRoutine.REQUEST:
    case routines.clearEventLogsRoutine.REQUEST:
      return {
        ...state,
        isFetching: true,
        error: null,
      };
    case routines.fetchEventLogsRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: action.payload || [],
        lastUpdated: new Date(),
      };
    case routines.createEventLogRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: [...state.data, action.payload],
        lastUpdated: new Date(),
      };
    case routines.deleteEventLogsRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: reject(state.data, log => {
          return includes(action.payload, log.key);
        }),
        lastUpdated: new Date(),
      };
    case routines.clearEventLogsRoutine.SUCCESS:
      return {
        ...state,
        isFetching: false,
        data: [],
        lastUpdated: new Date(),
      };
    case routines.fetchEventLogsRoutine.FAILURE:
    case routines.createEventLogRoutine.FAILURE:
    case routines.clearEventLogsRoutine.FAILURE:
      return {
        ...state,
        isFetching: false,
        error: action.payload,
      };
    case routines.fetchSettingsRoutine.REQUEST:
    case routines.updateSettingsRoutine.REQUEST:
      return {
        ...state,
        settings: {
          ...state.settings,
          isFetching: true,
          error: null,
        },
      };
    case routines.fetchSettingsRoutine.SUCCESS:
    case routines.updateSettingsRoutine.SUCCESS:
      return {
        ...state,
        settings: {
          ...state.settings,
          data: { ...state.settings.data, ...action.payload },
          isFetching: false,
          lastUpdated: new Date(),
        },
      };
    case routines.fetchSettingsRoutine.FAILURE:
    case routines.updateSettingsRoutine.FAILURE:
      return {
        ...state,
        settings: {
          ...state.settings,
          isFetching: false,
          error: action.payload,
        },
      };

    case routines.fetchNotificationsRoutine.REQUEST:
    case routines.updateNotificationsRoutine.REQUEST:
    case routines.clearNotificationsRoutine.REQUEST:
      return {
        ...state,
        notifications: {
          ...state.notifications,
          isFetching: true,
          error: null,
        },
      };
    case routines.fetchNotificationsRoutine.SUCCESS:
    case routines.updateNotificationsRoutine.SUCCESS:
      return {
        ...state,
        notifications: {
          ...state.notifications,
          data: { ...state.notifications.data, ...action.payload },
          isFetching: false,
          lastUpdated: new Date(),
        },
      };
    case routines.clearNotificationsRoutine.SUCCESS:
      return {
        ...state,
        notifications: {
          ...state.notifications,
          data: { ...initialState.notifications.data },
          isFetching: false,
          lastUpdated: new Date(),
        },
      };
    case routines.fetchNotificationsRoutine.FAILURE:
    case routines.updateNotificationsRoutine.FAILURE:
    case routines.clearNotificationsRoutine.FAILURE:
      return {
        ...state,
        notifications: {
          ...state.notifications,
          isFetching: false,
          error: action.payload,
        },
      };
    default:
      return state;
  }
};

export { reducer, initialState };
