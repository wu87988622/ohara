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

import React, { useState, useCallback, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import PropTypes from 'prop-types';

import { useWorkspace } from 'context/WorkspaceContext';
import TopicDataTable from './TopicDataTable';
import * as inspectApi from 'api/inspectApi';

const TopicDataWindow = props => {
  const { location } = props;
  const searchParams = new URLSearchParams(location.search);
  const { workspaceName } = useParams();
  const { findByWorkspaceName } = useWorkspace();

  const [isLoadedTopic, setIsLoadedTopic] = useState(false);
  const [topicData, setTopicData] = useState([]);

  const currentWorkspace = findByWorkspaceName(workspaceName);
  const topicName = searchParams.has('topic') ? searchParams.get('topic') : '';
  const topicLimit = searchParams.has('limit') ? searchParams.get('limit') : 10;
  const topicTimeout = searchParams.has('timeout')
    ? searchParams.get('timeout')
    : 5000;

  const fetchTopicData = useCallback(async () => {
    setIsLoadedTopic(true);
    let data = [];
    const response = await inspectApi.getTopicData({
      name: topicName,
      group: currentWorkspace.settings.name,
      limit: topicLimit,
      timeout: topicTimeout,
    });

    if (response) {
      data = response.messages.map(message => {
        // we don't need the "tags" field in the topic data
        if (message.value) delete message.value.tags;
        return message;
      });
    }

    setTopicData(data);
    setIsLoadedTopic(false);
  }, [topicName, topicLimit, topicTimeout, currentWorkspace]);

  useEffect(() => {
    if (!topicName || !currentWorkspace) return;
    fetchTopicData();
  }, [topicName, currentWorkspace, fetchTopicData]);

  // we don't generate the topic data view if no query parameters existed
  if (!currentWorkspace || !location.search || !topicName) return null;

  return <TopicDataTable topicData={topicData} isLoadedTopic={isLoadedTopic} />;
};

TopicDataWindow.propTypes = {
  location: PropTypes.shape({
    search: PropTypes.string,
  }).isRequired,
};

export default TopicDataWindow;
