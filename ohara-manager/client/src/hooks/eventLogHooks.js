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
import { useDispatch, useSelector } from 'react-redux';
import { isPlainObject } from 'lodash';

import * as context from 'context';
import * as selectors from 'store/selectors';
import * as actions from 'store/actions';

export const LOG_LEVEL = {
  info: 'info',
  error: 'error',
};

// action hooks
export const useFetchEventLogs = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.fetchEventLogs.trigger()), [
    dispatch,
  ]);
};
export const useDeleteEventLogs = () => {
  const dispatch = useDispatch();
  return useCallback(keys => dispatch(actions.deleteEventLogs.trigger(keys)), [
    dispatch,
  ]);
};
export const useClearEventLogs = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.clearEventLogs.trigger()), [
    dispatch,
  ]);
};
export const useUpdateEventSettings = () => {
  const dispatch = useDispatch();
  return useCallback(
    values => dispatch(actions.updateSettings.trigger(values)),
    [dispatch],
  );
};
export const useClearEventNotifications = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.clearNotifications.trigger()), [
    dispatch,
  ]);
};

// data selector
export const useEventLogs = () => {
  const getEventLogs = useMemo(selectors.makeGetEventLogs, []);
  return useSelector(useCallback(state => getEventLogs(state), [getEventLogs]));
};
export const useEventNotifications = () => {
  const getNotifications = useMemo(selectors.makeGetNotifications, []);
  return useSelector(
    useCallback(state => getNotifications(state), [getNotifications]),
  );
};
export const useEventSettings = () => {
  const getSettings = useMemo(selectors.makeGetSettings, []);
  return useSelector(useCallback(state => getSettings(state), [getSettings]));
};

export const useEventLog = () => {
  const useCreateEventLog = () => {
    const dispatch = useDispatch();
    return useCallback(
      values => dispatch(actions.createEventLog.trigger(values)),
      [dispatch],
    );
  };

  const createEventLog = useCreateEventLog();
  const clearEventLogs = useClearEventLogs();

  const showMessage = context.useSnackbar();

  return useMemo(() => {
    const eventLog = {
      info: (title, showSnackbar = true) => {
        createEventLog({ title, type: LOG_LEVEL.info });
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
          createEventLog(message, LOG_LEVEL.error);
        } else {
          createEventLog({ title, type: LOG_LEVEL.error });
        }
        if (showSnackbar) showMessage(title);
      },
      clear: () => clearEventLogs(),
    };
    return eventLog;
  }, [clearEventLogs, createEventLog, showMessage]);
};
