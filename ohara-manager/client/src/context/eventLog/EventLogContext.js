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

import React from 'react';
import PropTypes from 'prop-types';
import localForage from 'localforage';
import { isEmpty, get, take } from 'lodash';
import { createActions } from './eventLogActions';
import { useSettingsApi } from './eventLogHooks';
import { reducer, initialState } from './eventLogReducer';

const EventLogStateContext = React.createContext();
const EventLogDispatchContext = React.createContext();

const EventLogProvider = ({ children }) => {
  const [state, dispatch] = React.useReducer(reducer, initialState);
  const settingsApi = useSettingsApi();

  localForage.config({
    name: 'ohara',
    storeName: 'event_logs',
  });

  const actions = React.useMemo(
    () => createActions({ state, dispatch, settingsApi }),
    [state, dispatch, settingsApi],
  );

  React.useEffect(() => {
    if (!actions) return;
    actions.fetchSettings();
  }, [actions]);

  React.useEffect(() => {
    if (!actions) return;

    const unlimited = get(state, 'settings.data.unlimited');
    if (unlimited) return;

    const { data: logs = [] } = state;
    if (isEmpty(logs)) return;

    const limit = get(state, 'settings.data.limit');
    if (logs.length > limit) {
      const keysToDelete = take(logs, logs.length - limit).map(log => log.key);
      actions.deleteEventLogs(keysToDelete);
    }
  }, [state, actions]);

  return (
    <EventLogStateContext.Provider value={state}>
      <EventLogDispatchContext.Provider value={dispatch}>
        {children}
      </EventLogDispatchContext.Provider>
    </EventLogStateContext.Provider>
  );
};

const useEventLogState = () => {
  const context = React.useContext(EventLogStateContext);

  if (context === undefined) {
    throw new Error('useEventLogState must be used within a EventLogProvider');
  }
  return context;
};

const useEventLogDispatch = () => {
  const context = React.useContext(EventLogDispatchContext);
  if (context === undefined) {
    throw new Error(
      'useEventLogDispatch must be used within a EventLogProvider',
    );
  }
  return context;
};

EventLogProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

const useEventLogActions = () => {
  const state = useEventLogState();
  const dispatch = useEventLogDispatch();
  const settingsApi = useSettingsApi();
  return React.useMemo(() => createActions({ state, dispatch, settingsApi }), [
    state,
    dispatch,
    settingsApi,
  ]);
};

export {
  EventLogProvider,
  useEventLogState,
  useEventLogDispatch,
  useEventLogActions,
};
