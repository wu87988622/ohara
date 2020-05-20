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

import React, { useEffect, useState } from 'react';
import { isEmpty, delay, size, take } from 'lodash';

import StatusBar from 'components/common/StatusBar';
import EventLogList from './EventLogList';
import EventLogHeader from './EventLogHeader';

import Wrapper from './EventLogStyles';
import * as hooks from 'hooks';

const EventLog = () => {
  // used to keep filtered log
  const [logs, setLogs] = useState([]);
  const isFetching = hooks.useIsEventLogFetching();
  const { data: settings } = hooks.useEventSettings();
  const { data: notifications } = hooks.useEventNotifications();

  const clearNotifications = hooks.useClearEventNotificationsAction();
  const fetchEventLogs = hooks.useFetchEventLogsAction();
  const deleteEventLogs = hooks.useDeleteEventLogsAction();
  const [cleared, setCleared] = useState(false);

  useEffect(() => {
    fetchEventLogs();
  }, [fetchEventLogs]);

  React.useEffect(() => {
    const unlimited = settings?.unlimited;
    const limit = settings?.limit;

    if (unlimited || isEmpty(logs)) return;

    if (logs.length > limit) {
      const keysToDelete = take(logs, logs.length - limit).map(log => log.key);
      deleteEventLogs(keysToDelete);
    }
  }, [deleteEventLogs, logs, settings]);

  // When the event log page opens, should clear the event log notifications.
  useEffect(() => {
    if (!cleared && notifications.error > 0) {
      delay(() => clearNotifications(), 250);
      setCleared(true);
    }
  }, [cleared, clearNotifications, notifications.error, setCleared]);

  const handleFilter = filteredLogs => setLogs(filteredLogs);

  const getStatusText = () => {
    if (isFetching) return 'Loading...';
    const count = size(logs);
    if (!count) return 'No log';

    const limit = settings?.limit;
    const unlimited = settings?.unlimited;
    if (unlimited) {
      return `There are ${count} currently`;
    } else {
      return `There are ${count} currently (up to ${limit})`;
    }
  };

  return (
    <Wrapper>
      <EventLogHeader onFilter={handleFilter} />
      <div className="logs" data-testid="event-logs">
        <EventLogList data={logs} isFetching={isFetching} />
      </div>
      <div className="status-bar">
        <StatusBar>{getStatusText()}</StatusBar>
      </div>
    </Wrapper>
  );
};

export default EventLog;
