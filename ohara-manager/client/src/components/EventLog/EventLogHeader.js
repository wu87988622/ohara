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

import React, { useRef } from 'react';
import PropTypes from 'prop-types';
import { noop } from 'lodash';

import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import Typography from '@material-ui/core/Typography';
import IconButton from '@material-ui/core/IconButton';

import CloseIcon from '@material-ui/icons/Close';
import DeleteSweepIcon from '@material-ui/icons/DeleteSweep';
import SettingsIcon from '@material-ui/icons/Settings';

import { QuickSearch } from 'components/common/Search';
import { Tooltip } from 'components/common/Tooltip';
import Popover from 'components/common/Popover';
import { useEventLogDialog } from 'context';
import * as hooks from 'hooks';
import Wrapper from './EventLogHeaderStyles';
import EventLogSettings from './EventLogSettings';

const EventLogHeader = ({ onFilter }) => {
  const clearEventLogs = hooks.useClearEventLogsAction();
  const initEventLogs = hooks.useInitEventLogsAction();
  const { data: logs } = hooks.useEventLogs();
  const { close } = useEventLogDialog();
  const settingsPopoverRef = useRef(null);

  return (
    <Wrapper>
      <AppBar color="default" position="static">
        <Toolbar variant="dense">
          <Typography className="title" noWrap variant="h6">
            Event Logs
          </Typography>
          <QuickSearch
            className="search"
            data={logs}
            keys={['title', 'payload.errors[0].message']}
            setResults={onFilter}
            size="sm"
          />
          <Tooltip title="Clear event logs">
            <IconButton
              color="default"
              disabled={!logs || logs.length === 0}
              onClick={clearEventLogs}
            >
              <DeleteSweepIcon />
            </IconButton>
          </Tooltip>
          <Popover
            ref={settingsPopoverRef}
            showTooltip
            tooltipTitle="Event logs settings"
            trigger={
              <IconButton color="default">
                <SettingsIcon />
              </IconButton>
            }
          >
            <EventLogSettings
              onSave={() => settingsPopoverRef.current.close()}
            />
          </Popover>
          <Tooltip title="Close event logs">
            <IconButton
              color="default"
              onClick={() => {
                close();
                initEventLogs();
              }}
            >
              <CloseIcon />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>
    </Wrapper>
  );
};

EventLogHeader.propTypes = {
  onFilter: PropTypes.func,
};

EventLogHeader.defaultProps = {
  onFilter: noop,
};

export default EventLogHeader;
