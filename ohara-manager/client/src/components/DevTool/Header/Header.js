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
import React, { useState } from 'react';
import PropTypes from 'prop-types';

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
import FormControlLabel from '@material-ui/core/FormControlLabel';

import { Select } from 'components/common/Form';

import { StyledHeader, StyledSearchBody } from './HeaderStyles';
import { tabName } from '../DevToolDialog';

import * as logApi from 'api/logApi';

const getCurrentTime = () => {
  const today = new Date();
  const yyyy = today.getFullYear();
  const MM = String(today.getMonth() + 1).padStart(2, '0'); //January is 0!
  const dd = String(today.getDate()).padStart(2, '0');
  const hh = String(today.getHours());
  const mm = String(today.getMinutes());

  return yyyy + '-' + MM + '-' + dd + 'T' + hh + ':' + mm;
};

const Header = props => {
  const {
    tabIndex,
    topics,
    handleTabChange,
    closeDialog,
    setDataDispatch,
    ...others
  } = props;
  const { data, fetchTopicData, fetchLogs } = others;

  const [searchAnchor, setSearchAnchor] = useState(null);

  const handleSelectTopic = (event, object) => {
    setDataDispatch({ topicName: object.key });
  };

  const handleSelectService = (event, object) => {
    setDataDispatch({ service: object.key });
  };

  const handleSelectServiceNodes = (event, object) => {
    setDataDispatch({ hostname: object.key });
    const log = data.logs.find(log => log.hostname === object.key);
    if (log) {
      setDataDispatch({ hostLog: log.value.split('\n') });
    }
  };

  const handleSelectStreams = (event, object) => {
    setDataDispatch({ stream: object.key });
  };

  const handleRefresh = () => {
    if (tabIndex === tabName.topic) {
      fetchTopicData(data.topicLimit);
    } else if (tabIndex === tabName.logs) {
      fetchLogs();
    }
  };

  const handleSearchClick = event => {
    setSearchAnchor(event.currentTarget);
  };

  return (
    <StyledHeader
      container
      direction="row"
      justify="space-between"
      alignItems="center"
    >
      <Grid item xs={6} lg={4}>
        <Tabs
          value={tabIndex}
          indicatorColor="primary"
          textColor="primary"
          onChange={handleTabChange}
        >
          <Tab value={tabName.topic} label={tabName.topic.toUpperCase()} />
          <Tab value={tabName.log} label={tabName.log.toUpperCase()} />
        </Tabs>
      </Grid>
      <Grid item xs={6} lg={5}>
        <div className="items">
          <Select
            className={tabIndex === 'topics' ? '' : 'hidden'}
            input={{
              name: data.topicName,
              value: data.topicName,
              onChange: handleSelectTopic,
            }}
            disables={['Please select...']}
            list={topics.map(topic => topic.settings.name)}
            width="160px"
          />
          <Select
            className={tabIndex === 'logs' ? '' : 'hidden'}
            input={{
              name: data.service,
              value: data.service,
              onChange: handleSelectService,
            }}
            disables={['Please select...']}
            list={Object.keys(logApi.services)}
            width="160px"
          />
          {data.service === 'stream' ? (
            <Select
              className={tabIndex === 'logs' ? '' : 'hidden'}
              input={{
                name: data.stream,
                value: data.stream,
                onChange: handleSelectStreams,
              }}
              disables={['Please select...']}
              list={data.streams}
              width="160px"
            />
          ) : null}
          <Select
            className={tabIndex === 'logs' ? '' : 'hidden'}
            input={{
              name: data.hostname,
              value: data.hostname,
              onChange: handleSelectServiceNodes,
              disabled: isEmpty(data.service),
            }}
            disables={['Please select...']}
            list={data.logs.map(log => log.hostname)}
            width="160px"
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
              horizontal: 'center',
            }}
          >
            {tabIndex === tabName.topic ? (
              <StyledSearchBody tab={tabIndex}>
                <label>Rows per query</label>
                <TextField
                  type="number"
                  value={data.topicLimit}
                  onChange={event =>
                    setDataDispatch({ topicLimit: event.target.value })
                  }
                />
                <Button
                  variant="contained"
                  onClick={() => fetchTopicData(data.topicLimit)}
                >
                  QUERY
                </Button>
              </StyledSearchBody>
            ) : (
              <StyledSearchBody tab={tabIndex}>
                <RadioGroup>
                  <FormControlLabel
                    value="Latest"
                    control={<Radio color="primary" />}
                    label="Latest"
                  />
                  <label>Minutes per query</label>
                  <TextField
                    type="number"
                    value={data.timeRange}
                    onChange={event =>
                      setDataDispatch({ timeRange: event.target.value })
                    }
                  />
                </RadioGroup>
                <RadioGroup>
                  <FormControlLabel
                    value="Customize"
                    control={<Radio color="primary" />}
                    label="Customize"
                  />
                  <TextField
                    label="Start date"
                    type="datetime-local"
                    defaultValue={getCurrentTime()}
                    InputLabelProps={{
                      shrink: true,
                    }}
                  />
                  <TextField
                    label="End date"
                    type="datetime-local"
                    defaultValue={getCurrentTime()}
                    InputLabelProps={{
                      shrink: true,
                    }}
                  />
                </RadioGroup>
              </StyledSearchBody>
            )}
          </Popover>

          <OpenInNewIcon
            className="item"
            onClick={() => {
              if (data.topicName)
                window.open(
                  `${window.location}/view?topic=${data.topicName}&limit=${data.topicLimit}`,
                );
            }}
          />
          <CloseIcon className="item" onClick={closeDialog} />
        </div>
      </Grid>
    </StyledHeader>
  );
};

Header.propTypes = {
  tabIndex: PropTypes.string.isRequired,
  topics: PropTypes.array.isRequired,
  handleTabChange: PropTypes.func.isRequired,
  closeDialog: PropTypes.func.isRequired,
  setDataDispatch: PropTypes.func.isRequired,
  others: PropTypes.node,
};

export default Header;
