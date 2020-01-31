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

import { get, has } from 'lodash';
import localForage from 'localforage';
import * as routines from './eventLogRoutines';

export const createActions = context => {
  const { state, dispatch, eventLogApi } = context;

  return {
    fetchEventLogs: async () => {
      const routine = routines.fetchEventLogsRoutine;
      if (state.isFetching || state.lastUpdated || state.error) return;
      dispatch(routine.request());
      try {
        const data = [];
        await localForage.iterate((value, key) => {
          const log = {
            key,
            type: get(value, 'type'),
            title: get(value, 'payload.title'),
            createAt: get(value, 'createAt'),
            payload: get(value, 'payload'),
          };
          data.push(log);
        });
        dispatch(routine.success(data));
      } catch (e) {
        dispatch(routine.failure(e));
      }
    },
    createEventLog: async (log, type = 'info') => {
      const routine = routines.createEventLogRoutine;
      if (state.isFetching) return;
      if (!has(log, 'title')) {
        // eslint-disable-next-line no-console
        console.error('createEventLog error: must contain the title property');
        return;
      }

      try {
        const now = new Date();
        const key = now.getTime().toString();
        const value = {
          key,
          type,
          title: log.title,
          createAt: now,
          payload: log,
        };
        await localForage.setItem(key, value);
        dispatch(routine.success(value));
      } catch (e) {
        dispatch(routine.failure(e));
      }
    },
    deleteEventLogs: async (keys = []) => {
      const routine = routines.deleteEventLogsRoutine;
      if (state.isFetching) return;

      try {
        await Promise.all(
          keys.map(async key => await localForage.removeItem(key)),
        );
        dispatch(routine.success(keys));
      } catch (e) {
        dispatch(routine.failure(e));
      }
    },
    clearEventLogs: async () => {
      const routine = routines.clearEventLogsRoutine;
      if (state.isFetching) return;

      try {
        await localForage.clear();
        dispatch(routine.success());
      } catch (e) {
        dispatch(routine.failure(e));
      }
    },
    fetchSettings: async () => {
      const routine = routines.fetchSettingsRoutine;
      if (
        state.settings.isFetching ||
        state.settings.lastUpdated ||
        state.settings.error
      ) {
        return;
      }

      dispatch(routine.request());
      try {
        const data = await eventLogApi.fetchSettings();
        dispatch(routine.success(data));
      } catch (e) {
        dispatch(routine.failure(e.message));
      }
    },
    updateSettings: async values => {
      const routine = routines.updateSettingsRoutine;
      if (state.settings.isFetching) return;

      const ensuredValues = { ...state.settings.data, ...values };
      dispatch(routine.request());
      try {
        await eventLogApi.updateSettings(ensuredValues);
        dispatch(routine.success(ensuredValues));
      } catch (e) {
        dispatch(routine.failure(e.message));
      }
    },

    fetchNotifications: async () => {
      const routine = routines.fetchNotificationsRoutine;
      if (
        state.notifications.isFetching ||
        state.notifications.lastUpdated ||
        state.notifications.error
      ) {
        return;
      }

      dispatch(routine.request());
      try {
        const data = await eventLogApi.fetchNotifications();
        dispatch(routine.success(data));
      } catch (e) {
        dispatch(routine.failure(e.message));
      }
    },
    updateNotifications: async values => {
      const routine = routines.updateNotificationsRoutine;
      if (state.notifications.isFetching) return;

      const ensuredValues = { ...state.notifications.data, ...values };
      dispatch(routine.request());
      try {
        await eventLogApi.updateNotifications(ensuredValues);
        dispatch(routine.success(ensuredValues));
      } catch (e) {
        dispatch(routine.failure(e.message));
      }
    },
    clearNotifications: async () => {
      const routine = routines.clearNotificationsRoutine;
      if (state.notifications.isFetching) return;

      dispatch(routine.request());
      try {
        await eventLogApi.clearNotifications();
        dispatch(routine.success());
      } catch (e) {
        dispatch(routine.failure(e.message));
      }
    },
  };
};
