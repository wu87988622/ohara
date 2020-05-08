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
import { get, isEmpty } from 'lodash';
import IconButton from '@material-ui/core/IconButton';
import RefreshIcon from '@material-ui/icons/Refresh';
import SearchIcon from '@material-ui/icons/Search';
import OpenInNewIcon from '@material-ui/icons/OpenInNew';
import TextField from '@material-ui/core/TextField';
import Radio from '@material-ui/core/Radio';
import RadioGroup from '@material-ui/core/RadioGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';

import { KIND, LOG_TIME_GROUP } from 'const';
import { Tooltip } from 'components/common/Tooltip';
import { Button } from 'components/common/Form';
import Popover from 'components/common/Popover';
import Select from 'components/common/Select';
import { TAB } from 'context/devTool/const';
import { useCurrentLogs, useCurrentHosts } from 'components/DevTool/hooks';
import * as hooks from 'hooks';
import { LOG_SERVICES } from 'api/apiInterface/logInterface';
import { StyledSearchBody, StyledTextField } from './ControllerStyles';

const ControllerLog = () => {
  const shabondis = hooks.useShabondis();
  const streams = hooks.useStreams();

  const refetchLog = hooks.useRefetchDevToolLog();
  const setLogQueryParams = hooks.useSetDevToolLogQueryParams();
  const { query, isFetching } = hooks.useDevToolLog();
  const {
    hostName,
    logType,
    shabondiKey,
    streamKey,
    timeGroup,
    timeRange,
    startTime,
    endTime,
  } = query;
  const shabondiName = get(shabondiKey, 'name', '');
  const streamName = get(streamKey, 'name', '');

  const currentLog = useCurrentLogs();
  const hosts = useCurrentHosts();

  const handleOpenNewWindow = () => {
    if (currentLog) {
      // remove the trailing slash if no pathname
      const addSlash = window.location.pathname === '/' ? '' : '/';
      window.open(
        `${window.location}${addSlash}view?type=${TAB.log}&logType=${logType}`
          .concat(
            `&hostname=${hostName}&shabondiName=${shabondiName}&streamName=${streamName}`,
          )
          .concat(
            `&timeGroup=${timeGroup}&timeRange=${timeRange}&startTime=${startTime}&endTime=${endTime}`,
          ),
      );
    }
  };

  const handleRadioChange = event => {
    setLogQueryParams({ timeGroup: event.target.value });
    if (event.target.value === LOG_TIME_GROUP.customize) {
      // for initial the form, we need to set value explicitly
      setLogQueryParams({
        startTime: moment()
          .subtract(10, 'minutes')
          .format('YYYY-MM-DD[T]hh:mm'),
      });
      setLogQueryParams({ endTime: moment().format('YYYY-MM-DD[T]hh:mm') });
    }
  };

  const getDisableState = () => {
    // true -> disabled
    if (isFetching) return true;
    if (!logType) return true;
    if (logType === KIND.shabondi && isEmpty(shabondiKey)) return true;
    if (logType === KIND.stream && !isEmpty(streamKey)) return true;
  };

  return (
    <>
      <Select
        value={logType}
        onChange={event => setLogQueryParams({ logType: event.target.value })}
        list={Object.keys(LOG_SERVICES)}
        disabled={isFetching}
        testId="log-type-select"
      />
      {logType === KIND.shabondi && (
        <Select
          value={shabondiName}
          onChange={event =>
            setLogQueryParams({ shabondiName: event.target.value })
          }
          list={shabondis.map(shabondi => shabondi.name)}
          disabled={isFetching}
        />
      )}
      {logType === KIND.stream && (
        <Select
          value={streamName}
          onChange={event =>
            setLogQueryParams({ streamName: event.target.value })
          }
          list={streams.map(stream => stream.name)}
          disabled={isFetching}
        />
      )}

      <Select
        disabled={
          !logType ||
          (logType === KIND.shabondi && isEmpty(shabondiKey)) ||
          (logType === KIND.stream && !isEmpty(streamKey)) ||
          isFetching ||
          isEmpty(hostName)
        }
        value={hostName}
        onChange={event => setLogQueryParams({ hostName: event.target.value })}
        list={hosts}
      />
      <Tooltip title="Fetch the data again">
        <IconButton
          className="item"
          disabled={getDisableState()}
          onClick={refetchLog}
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
        <StyledSearchBody tab={TAB.log} data-testid="log-query-popover">
          <RadioGroup value={timeGroup} onChange={handleRadioChange}>
            <FormControlLabel
              value={LOG_TIME_GROUP.latest}
              control={<Radio color="primary" />}
              label="Latest"
              disabled={isEmpty(logType)}
            />
            <label>Minutes per query</label>
            <TextField
              disabled={isEmpty(logType) || timeGroup !== LOG_TIME_GROUP.latest}
              type="number"
              defaultValue={timeRange}
              onChange={event =>
                setLogQueryParams({ timeRange: Number(event.target.value) })
              }
            />
          </RadioGroup>
          <RadioGroup
            disabled={isEmpty(logType)}
            value={timeGroup}
            onChange={handleRadioChange}
          >
            <FormControlLabel
              value={LOG_TIME_GROUP.customize}
              control={<Radio color="primary" />}
              label="Customize"
              disabled={isEmpty(logType)}
            />
            <StyledTextField
              disabled={
                isEmpty(logType) || timeGroup !== LOG_TIME_GROUP.customize
              }
              label="Start date"
              type="datetime-local"
              defaultValue={
                startTime ||
                moment()
                  .subtract(10, 'minutes')
                  .format('YYYY-MM-DD[T]hh:mm')
              }
              inputProps={{
                max: endTime,
              }}
              InputLabelProps={{
                shrink: true,
              }}
              onChange={event =>
                setLogQueryParams({ startTime: event.target.value })
              }
            />
            <StyledTextField
              disabled
              label="End date"
              type="datetime-local"
              defaultValue={endTime || moment().format('YYYY-MM-DD[T]hh:mm')}
              InputLabelProps={{
                shrink: true,
              }}
              onChange={event =>
                setLogQueryParams({ endTime: event.target.value })
              }
            />
          </RadioGroup>
          <Button onClick={refetchLog} disabled={isEmpty(logType)}>
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
