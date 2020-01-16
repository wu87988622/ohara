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

import React, { useEffect } from 'react';
import { size } from 'lodash';

import { useEventLogActions, useEventLogState } from 'context';
import { VirtualizedList } from 'components/common/List';
import StatusBar from 'components/DevTool/StatusBar';
import EventLogRow from './EventLogRow';
import Header from './EventLogHeader';
import Wrapper from './EventLogStyles';

const EventLog = () => {
  const { data: logs, isFetching } = useEventLogState();
  const { fetchEventLogs } = useEventLogActions();

  useEffect(() => {
    fetchEventLogs();
  }, [fetchEventLogs]);

  const getStatusText = () => {
    if (isFetching) return 'Loading...';
    const count = size(logs);
    return count > 0 ? `Total ${count} logs` : 'No log';
  };

  return (
    <Wrapper>
      <Header />
      <div className="logs">
        <VirtualizedList
          data={logs}
          rowRenderer={EventLogRow}
          autoScrollToBottom
          isLoading={isFetching}
        />
      </div>
      <div className="status-bar">
        <StatusBar statusText={getStatusText()} />
      </div>
    </Wrapper>
  );
};

export default EventLog;
