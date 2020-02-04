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
import { delay, size } from 'lodash';

import { useEventLogActions, useEventLogState } from 'context';
import StatusBar from 'components/common/StatusBar';
import EventLogList from './EventLogList';
import EventLogHeader from './EventLogHeader';

import Wrapper from './EventLogStyles';

const EventLog = () => {
  const [logs, setLogs] = useState([]);
  const { isFetching, notifications } = useEventLogState();

  const { clearNotifications } = useEventLogActions();
  const [cleared, setCleared] = useState(false);

  // When the event log page opens, should clear the event log notifications.
  useEffect(() => {
    if (!cleared && notifications.data.error > 0) {
      delay(() => clearNotifications(), 250);
      setCleared(true);
    }
  }, [cleared, clearNotifications, notifications.data.error, setCleared]);

  const handleFilter = filteredLogs => setLogs(filteredLogs);

  const getStatusText = () => {
    if (isFetching) return 'Loading...';
    const count = size(logs);
    return count > 0 ? `Total ${count} logs` : 'No log';
  };

  return (
    <Wrapper>
      <EventLogHeader onFilter={handleFilter} />
      <div className="logs">
        <EventLogList data={logs} isFetching={isFetching} />
      </div>
      <div className="status-bar">
        <StatusBar>{getStatusText()}</StatusBar>
      </div>
    </Wrapper>
  );
};

export default EventLog;
