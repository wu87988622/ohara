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

import { isEmpty } from 'lodash';
import moment from 'moment';
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import RefreshIcon from '@material-ui/icons/Refresh';
import SearchIcon from '@material-ui/icons/Search';
import OpenInNewIcon from '@material-ui/icons/OpenInNew';
import CloseIcon from '@material-ui/icons/Close';
import Tab from '@material-ui/core/Tab';
import Tabs from '@material-ui/core/Tabs';
import Grid from '@material-ui/core/Grid';
import Popover from '@material-ui/core/Popover';
import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import Radio from '@material-ui/core/Radio';
import RadioGroup from '@material-ui/core/RadioGroup';
import Typography from '@material-ui/core/Typography';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import FormControl from '@material-ui/core/FormControl';
import Select from '@material-ui/core/Select';
import MenuItem from '@material-ui/core/MenuItem';
import InputBase from '@material-ui/core/InputBase';

import { StyledHeader, StyledSearchBody } from './HeaderStyles';
import { tabName } from '../DevToolDialog';

import * as logApi from 'api/logApi';

const StyledTextField = styled(TextField)`
  width: 230px;
`;

const BootstrapInput = styled(InputBase)(
  ({ theme }) => css`
    width: 160px;
    border-width: 1px;
    border-style: solid;
    border-color: ${theme.palette.grey[200]};
    margin: ${theme.spacing(1)}px;
  `,
);
const DialogSelect = props => {
  const {
    index,
    currentTab,
    value,
    onChange,
    list,
    setAnchor,
    anchor = null,
    disabled = false,
  } = props;
  return (
    <Typography hidden={index !== currentTab}>
      <FormControl disabled={disabled}>
        <Select
          value={value}
          onOpen={setAnchor}
          onChange={onChange}
          input={<BootstrapInput />}
          MenuProps={{
            getContentAnchorEl: null,
            anchorEl: anchor,
            anchorOrigin: {
              vertical: 'bottom',
              horizontal: 'center',
            },
            transformOrigin: {
              vertical: 'top',
              horizontal: 'center',
            },
          }}
        >
          {list.map(item => {
            return (
              <MenuItem value={item} key={item}>
                {item}
              </MenuItem>
            );
          })}
        </Select>
      </FormControl>
    </Typography>
  );
};
DialogSelect.propTypes = {
  index: PropTypes.string.isRequired,
  currentTab: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  list: PropTypes.array.isRequired,
  setAnchor: PropTypes.func.isRequired,
  anchor: PropTypes.any,
  disabled: PropTypes.bool,
};

const Header = props => {
  const { tabIndex, data, topics, ...others } = props;
  const {
    handleTabChange,
    closeDialog,
    setDataDispatch,
    fetchTopicData,
    fetchLogs,
    pipelineName,
  } = others;

  const [timeSeconds, setTimeSeconds] = useState(600);
  const [selectAnchor, setSelectAnchor] = useState(null);
  const [searchAnchor, setSearchAnchor] = useState(null);
  const [searchTimeGroup, setSearchTimeGroup] = useState('latest');

  const handleSelectAnchor = event => {
    setSelectAnchor(event.currentTarget);
  };

  const handleSelectTopic = (event, object) => {
    setDataDispatch({ topicName: object.key });
  };

  const handleSelectService = (event, object) => {
    setDataDispatch({ service: object.key });
  };

  const handleSelectServiceNodes = (event, object) => {
    setDataDispatch({ hostname: object.key });
  };

  const handleSelectStreams = (event, object) => {
    setDataDispatch({ stream: object.key });
  };

  const handleRefresh = () => {
    if (tabIndex === tabName.topic) {
      fetchTopicData(data.topicLimit);
    } else {
      fetchLogs(timeSeconds, data.hostname);
    }
  };

  const handleSearchClick = event => {
    setSearchAnchor(event.currentTarget);
  };

  const handleRadioChange = event => {
    setSearchTimeGroup(event.target.value);
    if (
      event.target.value === 'customize' &&
      !data.startTime &&
      !data.endTime
    ) {
      // for initial the form, we need to set value explicitly
      setDataDispatch({
        startTime: moment()
          .subtract(10, 'minutes')
          .format('YYYY-MM-DD[T]hh:mm'),
      });
      setDataDispatch({
        endTime: moment().format('YYYY-MM-DD[T]hh:mm'),
      });
    }
  };

  const handleButtonClick = () => {
    if (tabIndex === tabName.topic) {
      fetchTopicData(data.topicLimit);
    } else {
      let time = 0;
      if (searchTimeGroup === 'latest') {
        // timeRange uses minute units
        time = data.timeRange * 60;
      } else {
        time = Math.ceil(
          moment
            .duration(moment(data.endTime).diff(moment(data.startTime)))
            .asSeconds(),
        );
      }

      fetchLogs(time, data.hostname);
      setTimeSeconds(time);
      setDataDispatch({ timeGroup: searchTimeGroup });
    }
  };

  const handleOpenNewWindow = () => {
    if (tabIndex === tabName.topic) {
      if (data.topicName)
        window.open(
          `${window.location}/view?type=${tabName.topic}&topic=${data.topicName}&limit=${data.topicLimit}`,
        );
    } else {
      if (data.hostLog) {
        window.open(
          `${window.location}/view?type=${tabName.log}&service=${data.service}`
            .concat(`&hostname=${data.hostname}&timeSeconds=${timeSeconds}`)
            .concat(`&stream=${data.stream}&pipelineName=${pipelineName}`),
        );
      }
    }
  };

  return (
    <StyledHeader
      container
      direction="row"
      justify="space-between"
      alignItems="center"
    >
      <Grid item xs={4} lg={4}>
        <Tabs
          value={tabIndex}
          indicatorColor="primary"
          textColor="primary"
          onChange={handleTabChange}
        >
          <Tab value={tabName.topic} label={tabName.topic} />
          <Tab value={tabName.log} label={tabName.log} />
        </Tabs>
      </Grid>
      <Grid item xs={8} lg={7}>
        <div className="items">
          <DialogSelect
            index={tabName.topic}
            currentTab={tabIndex}
            value={data.topicName}
            onChange={handleSelectTopic}
            list={topics.map(topic => topic.settings.name)}
            setAnchor={handleSelectAnchor}
            anchor={selectAnchor}
          />
          <DialogSelect
            index={tabName.log}
            currentTab={tabIndex}
            value={data.service}
            onChange={handleSelectService}
            list={Object.keys(logApi.services)}
            setAnchor={handleSelectAnchor}
            anchor={selectAnchor}
          />
          {data.service === 'stream' ? (
            <DialogSelect
              index={tabName.log}
              currentTab={tabIndex}
              value={data.stream}
              onChange={handleSelectStreams}
              list={data.streams}
              setAnchor={handleSelectAnchor}
              anchor={selectAnchor}
            />
          ) : null}
          <DialogSelect
            disabled={
              !data.service || (data.service === 'stream' && !data.stream)
            }
            index={tabName.log}
            currentTab={tabIndex}
            value={data.hostname}
            onChange={handleSelectServiceNodes}
            list={data.hosts}
            setAnchor={handleSelectAnchor}
            anchor={selectAnchor}
          />
          <RefreshIcon className="item" onClick={handleRefresh} />
          <SearchIcon className="item" onClick={handleSearchClick} />
          <Popover
            open={Boolean(searchAnchor)}
            anchorEl={searchAnchor}
            onClose={() => setSearchAnchor(null)}
            anchorOrigin={{
              vertical: 'bottom',
              horizontal: 'center',
            }}
            transformOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
          >
            {tabIndex === tabName.topic ? (
              <StyledSearchBody tab={tabIndex}>
                <label>Rows per query</label>
                <TextField
                  type="number"
                  value={data.topicLimit}
                  onChange={event =>
                    setDataDispatch({ topicLimit: Number(event.target.value) })
                  }
                  disabled={isEmpty(data.topicName)}
                />
                <Button
                  variant="contained"
                  onClick={handleButtonClick}
                  disabled={isEmpty(data.topicName)}
                >
                  QUERY
                </Button>
              </StyledSearchBody>
            ) : (
              <StyledSearchBody tab={tabIndex}>
                <RadioGroup
                  value={searchTimeGroup}
                  onChange={handleRadioChange}
                >
                  <FormControlLabel
                    value="latest"
                    control={<Radio color="primary" />}
                    label="Latest"
                    disabled={isEmpty(data.service)}
                  />
                  <label>Minutes per query</label>
                  <TextField
                    disabled={
                      isEmpty(data.service) || searchTimeGroup !== 'latest'
                    }
                    type="number"
                    value={data.timeRange}
                    onChange={event =>
                      setDataDispatch({ timeRange: Number(event.target.value) })
                    }
                  />
                </RadioGroup>
                <RadioGroup
                  disabled={isEmpty(data.service)}
                  value={searchTimeGroup}
                  onChange={handleRadioChange}
                >
                  <FormControlLabel
                    value="customize"
                    control={<Radio color="primary" />}
                    label="Customize"
                    disabled={isEmpty(data.service)}
                  />
                  <StyledTextField
                    disabled={
                      isEmpty(data.service) || searchTimeGroup !== 'customize'
                    }
                    label="Start date"
                    type="datetime-local"
                    value={
                      data.startTime ||
                      moment()
                        .subtract(10, 'minutes')
                        .format('YYYY-MM-DD[T]hh:mm')
                    }
                    InputLabelProps={{
                      shrink: true,
                    }}
                    onChange={event =>
                      setDataDispatch({ startTime: event.target.value })
                    }
                  />
                  <StyledTextField
                    disabled
                    label="End date"
                    type="datetime-local"
                    value={
                      data.endTime || moment().format('YYYY-MM-DD[T]hh:mm')
                    }
                    InputLabelProps={{
                      shrink: true,
                    }}
                    onChange={event =>
                      setDataDispatch({ endTime: event.target.value })
                    }
                  />
                </RadioGroup>
                <Button
                  variant="contained"
                  onClick={handleButtonClick}
                  disabled={isEmpty(data.service)}
                >
                  QUERY
                </Button>
              </StyledSearchBody>
            )}
          </Popover>

          <OpenInNewIcon className="item" onClick={handleOpenNewWindow} />
          <CloseIcon className="item" onClick={closeDialog} />
        </div>
      </Grid>
    </StyledHeader>
  );
};

Header.propTypes = {
  tabIndex: PropTypes.string.isRequired,
  data: PropTypes.shape({
    service: PropTypes.string.isRequired,
    topicLimit: PropTypes.number,
    topicName: PropTypes.string,
    hosts: PropTypes.array,
    streams: PropTypes.array,
    stream: PropTypes.string,
    hostname: PropTypes.string,
    timeRange: PropTypes.number,
    startTime: PropTypes.string,
    endTime: PropTypes.string,
    hostLog: PropTypes.array,
  }),
  topics: PropTypes.array.isRequired,
  handleTabChange: PropTypes.func.isRequired,
  closeDialog: PropTypes.func.isRequired,
  setDataDispatch: PropTypes.func.isRequired,
  others: PropTypes.node,
};

export default Header;
