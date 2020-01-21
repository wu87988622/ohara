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
import moment from 'moment';
import { has, isEmpty } from 'lodash';
import IconButton from '@material-ui/core/IconButton';
import RefreshIcon from '@material-ui/icons/Refresh';
import SearchIcon from '@material-ui/icons/Search';
import OpenInNewIcon from '@material-ui/icons/OpenInNew';
import TextField from '@material-ui/core/TextField';
import Radio from '@material-ui/core/Radio';
import RadioGroup from '@material-ui/core/RadioGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';

import { KIND } from 'const';
import { Tooltip } from 'components/common/Tooltip';
import { Button } from 'components/common/Form';
import Popover from 'components/common/Popover';
import Select from 'components/common/Select';
import { TAB } from 'context/devTool/const';
import {
  useCurrentLogs,
  useCurrentHosts,
  useCurrentHostName,
} from 'components/DevTool/hooks';
import * as context from 'context';
import { TIME_GROUP } from 'context/log/const';
import { logServices } from 'api/logApi';
import { usePrevious } from 'utils/hooks';
import { StyledSearchBody, StyledTextField } from './ControllerStyles';

const ControllerLog = () => {
  const { data: streams } = context.useStreamState();

  const logActions = context.useLogActions();
  const { query, isFetching, lastUpdated } = context.useLogState();
  const {
    logType,
    streamName,
    timeGroup,
    timeRange,
    startTime,
    endTime,
  } = query;

  const currentLog = useCurrentLogs();
  const hosts = useCurrentHosts();
  const hostname = useCurrentHostName();

  const prevLogType = usePrevious(logType);

  React.useEffect(() => {
    if (lastUpdated && prevLogType === logType) return;
    if (isEmpty(logType)) return;

    const getTimeSeconds = () => {
      if (timeGroup === TIME_GROUP.latest) {
        // timeRange uses minute units
        return timeRange * 60;
      } else {
        return Math.ceil(
          moment.duration(moment(endTime).diff(moment(startTime))).asSeconds(),
        );
      }
    };

    const setHostName = result => {
      if (has(result, 'data')) {
        if (!isEmpty(result.data.logs)) {
          logActions.setHostName(result.data.logs[0].hostname);
        }
      } else {
        logActions.setHostName('');
      }
    };

    switch (logType) {
      case KIND.configurator:
        logActions
          .fetchConfiguratorLog(getTimeSeconds())
          .then(result => setHostName(result));
        break;
      case KIND.zookeeper:
        logActions
          .fetchZookeeperLog(getTimeSeconds())
          .then(result => setHostName(result));
        break;
      case KIND.broker:
        logActions
          .fetchBrokerLog(getTimeSeconds())
          .then(result => setHostName(result));
        break;
      case KIND.worker:
        logActions
          .fetchWorkerLog(getTimeSeconds())
          .then(result => setHostName(result));
        break;
      case KIND.stream:
        if (isEmpty(streamName)) return;
        logActions
          .fetchStreamLog({
            name: streamName,
            sinceSeconds: getTimeSeconds(),
          })
          .then(result => setHostName(result));
        break;
      default:
    }
  }, [
    endTime,
    lastUpdated,
    logActions,
    logType,
    prevLogType,
    startTime,
    streamName,
    timeGroup,
    timeRange,
  ]);

  const handleOpenNewWindow = () => {
    if (currentLog) {
      // remove the trailing slash if no pathname
      const addSlash = window.location.pathname === '/' ? '' : '/';
      window.open(
        `${window.location}${addSlash}view?type=${TAB.log}&logType=${logType}`
          .concat(`&hostname=${hostname}&streamName=${streamName}`)
          .concat(
            `&timeGroup=${timeGroup}&timeRange=${timeRange}&startTime=${startTime}&endTime=${endTime}`,
          ),
      );
    }
  };

  const handleRadioChange = event => {
    logActions.setTimeGroup(event.target.value);
    if (event.target.value === TIME_GROUP.customize && !startTime && !endTime) {
      // for initial the form, we need to set value explicitly
      logActions.setStartTime(
        moment()
          .subtract(10, 'minutes')
          .format('YYYY-MM-DD[T]hh:mm'),
      );
      logActions.setEndTime(moment().format('YYYY-MM-DD[T]hh:mm'));
    }
  };

  const getDisableState = () => {
    // true -> disabled
    if (isFetching) return true;
    if (!logType) return true;
    if (logType === KIND.stream && !streamName) return true;
  };

  return (
    <>
      <Select
        value={logType}
        onChange={event => {
          logActions.setLogType(event.target.value);
          logActions.setHostName('');
        }}
        list={Object.keys(logServices)}
        disabled={isFetching}
      />
      {logType === KIND.stream && (
        <Select
          value={streamName}
          onChange={event => logActions.setStreamName(event.target.value)}
          list={streams}
          disabled={isFetching}
        />
      )}

      <Select
        disabled={
          !logType ||
          (logType === KIND.stream && !streamName) ||
          isFetching ||
          isEmpty(hostname)
        }
        value={hostname}
        onChange={event => logActions.setHostName(event.target.value)}
        list={hosts}
      />
      <Tooltip title="Fetch the data again">
        <IconButton
          className="item"
          disabled={getDisableState()}
          onClick={logActions.refetchLog}
          size="small"
        >
          <RefreshIcon />
        </IconButton>
      </Tooltip>
      <Popover
        showTooltip
        tooltipTitle="Query with different parameters"
        trigger={
          <IconButton
            disabled={getDisableState()}
            className="item"
            size="small"
          >
            <SearchIcon />
          </IconButton>
        }
      >
        <StyledSearchBody tab={TAB.log}>
          <RadioGroup value={timeGroup} onChange={handleRadioChange}>
            <FormControlLabel
              value={TIME_GROUP.latest}
              control={<Radio color="primary" />}
              label="Latest"
              disabled={isEmpty(logType)}
            />
            <label>Minutes per query</label>
            <TextField
              disabled={isEmpty(logType) || timeGroup !== TIME_GROUP.latest}
              type="number"
              defaultValue={timeRange}
              onChange={event =>
                logActions.setTimeRange(Number(event.target.value))
              }
            />
          </RadioGroup>
          <RadioGroup
            disabled={isEmpty(logType)}
            value={timeGroup}
            onChange={handleRadioChange}
          >
            <FormControlLabel
              value={TIME_GROUP.customize}
              control={<Radio color="primary" />}
              label="Customize"
              disabled={isEmpty(logType)}
            />
            <StyledTextField
              disabled={isEmpty(logType) || timeGroup !== TIME_GROUP.customize}
              label="Start date"
              type="datetime-local"
              defaultValue={
                startTime ||
                moment()
                  .subtract(10, 'minutes')
                  .format('YYYY-MM-DD[T]hh:mm')
              }
              InputLabelProps={{
                shrink: true,
              }}
              onChange={event => logActions.setStartTime(event.target.value)}
            />
            <StyledTextField
              disabled
              label="End date"
              type="datetime-local"
              defaultValue={endTime || moment().format('YYYY-MM-DD[T]hh:mm')}
              InputLabelProps={{
                shrink: true,
              }}
              onChange={event => logActions.setEndTime(event.target.value)}
            />
          </RadioGroup>
          <Button onClick={logActions.refetchLog} disabled={isEmpty(logType)}>
            QUERY
          </Button>
        </StyledSearchBody>
      </Popover>
      <Tooltip title="Open in a new window">
        <IconButton
          className="item"
          onClick={handleOpenNewWindow}
          size="small"
          disabled={getDisableState()}
        >
          <OpenInNewIcon />
        </IconButton>
      </Tooltip>
    </>
  );
};

export { ControllerLog };
