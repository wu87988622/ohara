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

  const handleRadioChange = (event) => {
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
    if (logType === KIND.stream && isEmpty(streamKey)) return true;
    if (isEmpty(hostName)) return true;
  };

  return (
    <>
      <Select
        disabled={isFetching}
        list={Object.keys(LOG_SERVICES)}
        onChange={(event) => setLogQueryParams({ logType: event.target.value })}
        testId="log-type-select"
        value={logType}
      />
      {logType === KIND.shabondi && (
        <Select
          disabled={isFetching || isEmpty(shabondis)}
          list={shabondis.map((shabondi) => shabondi.name)}
          onChange={(event) =>
            setLogQueryParams({ shabondiName: event.target.value })
          }
          testId="log-cell-select"
          value={shabondiName}
        />
      )}
      {logType === KIND.stream && (
        <Select
          disabled={isFetching || isEmpty(streams)}
          list={streams.map((stream) => stream.name)}
          onChange={(event) =>
            setLogQueryParams({ streamName: event.target.value })
          }
          testId="log-cell-select"
          value={streamName}
        />
      )}

      <Select
        disabled={getDisableState()}
        list={hosts}
        onChange={(event) =>
          setLogQueryParams({ hostName: event.target.value })
        }
        testId="log-hostname-select"
        value={hostName}
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
            className="item"
            disabled={getDisableState()}
            size="small"
          >
            <SearchIcon />
          </IconButton>
        }
      >
        <StyledSearchBody data-testid="log-query-popover" tab={TAB.log}>
          <RadioGroup onChange={handleRadioChange} value={timeGroup}>
            <FormControlLabel
              control={<Radio color="primary" />}
              disabled={isEmpty(logType)}
              label="Latest"
              value={LOG_TIME_GROUP.latest}
            />
            <label>Minutes per query</label>
            <TextField
              defaultValue={timeRange}
              disabled={isEmpty(logType) || timeGroup !== LOG_TIME_GROUP.latest}
              onChange={(event) =>
                setLogQueryParams({ timeRange: Number(event.target.value) })
              }
              type="number"
            />
          </RadioGroup>
          <RadioGroup
            disabled={isEmpty(logType)}
            onChange={handleRadioChange}
            value={timeGroup}
          >
            <FormControlLabel
              control={<Radio color="primary" />}
              disabled={isEmpty(logType)}
              label="Customize"
              value={LOG_TIME_GROUP.customize}
            />
            <StyledTextField
              defaultValue={
                startTime ||
                moment().subtract(10, 'minutes').format('YYYY-MM-DD[T]hh:mm')
              }
              disabled={
                isEmpty(logType) || timeGroup !== LOG_TIME_GROUP.customize
              }
              id="start-date"
              InputLabelProps={{
                shrink: true,
              }}
              inputProps={{
                max: endTime,
              }}
              label="Start date"
              onChange={(event) =>
                setLogQueryParams({ startTime: event.target.value })
              }
              type="datetime-local"
            />
            <StyledTextField
              defaultValue={endTime || moment().format('YYYY-MM-DD[T]hh:mm')}
              disabled
              id="end-date"
              InputLabelProps={{
                shrink: true,
              }}
              label="End date"
              onChange={(event) =>
                setLogQueryParams({ endTime: event.target.value })
              }
              type="datetime-local"
            />
          </RadioGroup>
          <Button disabled={isEmpty(logType)} onClick={refetchLog}>
            QUERY
          </Button>
        </StyledSearchBody>
      </Popover>
      <Tooltip title="Open in a new window">
        <IconButton
          className="item"
          disabled={getDisableState()}
          onClick={handleOpenNewWindow}
          size="small"
        >
          <OpenInNewIcon />
        </IconButton>
      </Tooltip>
    </>
  );
};

export { ControllerLog };
