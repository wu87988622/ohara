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

import { useCallback, useMemo } from 'react';
import { isPlainObject, min } from 'lodash';

import * as context from 'context';
import { useLocalStorage } from 'utils/hooks';
import { initialState } from './eventLogReducer';

export const useEventLogApi = () => {
  const [settings, setSettings] = useLocalStorage(
    'event_log_settings',
    initialState.settings.data,
  );

  const [notifications, setNotifications] = useLocalStorage(
    'event_log_notifications',
    initialState.notifications.data,
  );

  return useMemo(() => {
    const api = {
      fetchSettings: () => settings,
      updateSettings: setSettings,
      fetchNotifications: () => notifications,
      updateNotifications: setNotifications,
      clearNotifications: () => {
        setNotifications(initialState.notifications.data);
      },
    };
    return api;
  }, [settings, setSettings, notifications, setNotifications]);
};

export const useEventLog = () => {
  const {
    createEventLog,
    clearEventLogs,
    updateNotifications,
  } = context.useEventLogActions();
  const showMessage = context.useSnackbar();

  const { notifications, settings } = context.useEventLogState();
  const { error = 0, info = 0 } = notifications.data;
  const { limit = 1000, unlimited } = settings.data;

  const { isOpen: isEventLogDialogOpen } = context.useEventLogDialog();

  const increaseErrorNotification = useCallback(() => {
    const nextCount = error + 1;
    const countToUpdate = unlimited ? nextCount : min([nextCount, limit]);
    updateNotifications({ error: countToUpdate });
  }, [error, limit, unlimited, updateNotifications]);

  const increaseInfoNotification = useCallback(() => {
    const nextCount = info + 1;
    const countToUpdate = unlimited ? nextCount : min([nextCount, limit]);
    updateNotifications({ info: countToUpdate });
  }, [info, limit, unlimited, updateNotifications]);

  return useMemo(() => {
    const eventLog = {
      info: (title, showSnackbar = true) => {
        createEventLog({ title }, 'info');
        if (!isEventLogDialogOpen) increaseInfoNotification();
        if (showSnackbar) showMessage(title);
      },
      /**
       * @param {String|Object} message The message of error.
       * @example
       *
       * error('Failed to create topic t1');
       *
       * error({title: 'Failed to create topic t1'});
       */
      error: (message, showSnackbar = true) => {
        const title = isPlainObject(message) ? message.title : message;
        if (isPlainObject(message)) {
          createEventLog(message, 'error');
        } else {
          createEventLog({ title }, 'error');
        }
        if (!isEventLogDialogOpen) increaseErrorNotification();
        if (showSnackbar) showMessage(title);
      },
      clear: () => clearEventLogs(),
    };
    return eventLog;
  }, [
    clearEventLogs,
    createEventLog,
    increaseErrorNotification,
    increaseInfoNotification,
    isEventLogDialogOpen,
    showMessage,
  ]);
};
