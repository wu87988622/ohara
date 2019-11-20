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
import React, { useEffect, useState, useCallback } from 'react';
import PropTypes from 'prop-types';
import { useParams } from 'react-router-dom';
import RefreshIcon from '@material-ui/icons/Refresh';
import SearchIcon from '@material-ui/icons/Search';
import OpenInNewIcon from '@material-ui/icons/OpenInNew';
import CloseIcon from '@material-ui/icons/Close';
import Tab from '@material-ui/core/Tab';
import Tabs from '@material-ui/core/Tabs';
import Button from '@material-ui/core/Button';
import Popover from '@material-ui/core/Popover';
import { TextField } from '@material-ui/core';

import { Select } from 'components/common/Form';
import { useDevToolDialog } from 'context/DevToolDialogContext';
import { useWorkspace, useTopicState, useTopicActions } from 'context';
import TopicDataTable from './TopicDataTable';
import { DevTool, SearchBody } from './Styles';
import * as inspectApi from 'api/inspectApi';

const DevToolDialog = () => {
  const { workspaceName } = useParams();
  const { findByWorkspaceName } = useWorkspace();
  const { data: topics } = useTopicState();
  const { fetchTopics } = useTopicActions();
  const { isOpen, setIsOpen } = useDevToolDialog();
  const [tabIndex, setTabIndex] = useState('topics');
  const [topicLimit, setTopicLimit] = useState(10);
  const [topicName, setTopicName] = useState('');
  const [topicData, setTopicData] = useState([]);
  const [isLoadedTopic, setIsLoadedTopic] = useState(false);
  const [searchAnchor, setSearchAnchor] = useState(null);

  const currentWorkspace = findByWorkspaceName(workspaceName);

  useEffect(() => {
    if (isEmpty(currentWorkspace)) return;
    fetchTopics(currentWorkspace.settings.name);
  }, [fetchTopics, currentWorkspace]);

  const fetchTopicData = useCallback(
    async (topicLimit = 10) => {
      setIsLoadedTopic(true);
      const response = await inspectApi.getTopicData({
        name: topicName,
        group: currentWorkspace.settings.name,
        limit: topicLimit,
        timeout: 5000,
      });

      const data = response.messages.map(message => {
        // we don't need the "tags" field in the topic data
        if (message.value) delete message.value.tags;
        return message;
      });

      setTopicData(data);
      setIsLoadedTopic(false);
    },
    [topicName, currentWorkspace],
  );

  useEffect(() => {
    if (isEmpty(topicName) || isEmpty(currentWorkspace)) return;
    fetchTopicData();
  }, [topicName, currentWorkspace, fetchTopicData]);

  const TabPanel = props => {
    const { children, value, index } = props;

    return (
      <div className="tab-body" hidden={value !== index}>
        {children}
      </div>
    );
  };

  TabPanel.propTypes = {
    children: PropTypes.node.isRequired,
    index: PropTypes.string.isRequired,
    value: PropTypes.node.isRequired,
  };

  const handleTabChange = (event, newTabIndex) => {
    if (currentWorkspace && newTabIndex === 'topics')
      fetchTopics(currentWorkspace.settings.name);
    setTabIndex(newTabIndex);
  };

  const handleSelectTopic = (event, object) => {
    setTopicName(object.key);
  };

  const handleSearchClick = event => {
    setSearchAnchor(event.currentTarget);
  };

  return (
    <DevTool hidden={!isOpen}>
      <div className="header">
        <Tabs
          value={tabIndex}
          indicatorColor="primary"
          textColor="primary"
          onChange={handleTabChange}
        >
          <Tab value="topics" label="TOPICS" />
          <Tab value="logs" label="LOGS" />
        </Tabs>
        <div className="items">
          <Select
            input={{
              name: topicName,
              value: topicName,
              onChange: handleSelectTopic,
            }}
            disables={['Please select...']}
            list={topics.map(topic => topic.settings.name)}
          />
          <RefreshIcon
            className="item"
            onClick={() => fetchTopicData(topicLimit)}
          />
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
            <SearchBody>
              <label>Rows per query</label>
              <TextField
                type="number"
                value={topicLimit}
                onChange={event => setTopicLimit(event.target.value)}
              />
              <Button
                variant="contained"
                onClick={() => fetchTopicData(topicLimit)}
              >
                QUERY
              </Button>
            </SearchBody>
          </Popover>
          <OpenInNewIcon
            className="item"
            onClick={() => {
              if (topicName)
                window.open(
                  `${window.location}/view?topic=${topicName}&limit=${topicLimit}`,
                );
            }}
          />
          <CloseIcon className="item" onClick={() => setIsOpen(false)} />
        </div>
      </div>
      <TabPanel value={tabIndex} index={'topics'}>
        <TopicDataTable topicData={topicData} isLoadedTopic={isLoadedTopic} />
      </TabPanel>
      <TabPanel value={tabIndex} index={'logs'}>
        Logs
      </TabPanel>
    </DevTool>
  );
};

export default DevToolDialog;
