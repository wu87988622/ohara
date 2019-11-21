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
import React, {
  useEffect,
  useState,
  useCallback,
  useReducer,
  useRef,
} from 'react';
import styled, { css } from 'styled-components';
import { useParams } from 'react-router-dom';

import { useDevToolDialog, useTopicState, useTopicActions } from 'context';
import * as inspectApi from 'api/inspectApi';
import * as logApi from 'api/logApi';
import * as brokerApi from 'api/brokerApi';
import * as streamApi from 'api/streamApi';
import Header from './Header';
import Body from './Body';

const StyledDevTool = styled.div(
  ({ theme }) => css`
    position: absolute;
    left: 70px;
    width: calc(100% - 70px);
    min-width: 956px;
    height: 468px;
    bottom: 0;
    z-index: ${theme.zIndex.modal};
    background-color: ${theme.palette.common.white};

    &.is-close {
      display: none;
    }

    .header {
      width: 100%;
      height: 48px;
      background-color: ${theme.palette.grey[50]};
    }
  `,
);

export const tabName = {
  topic: 'topics',
  log: 'logs',
};

const initialState = {
  type: tabName.topic,
  isLoading: false,
  // topics tab
  topicName: '',
  topicLimit: 10,
  topicData: [],
  // logs tab
  service: '',
  hostname: '',
  hostLog: [],
  logs: [],
  stream: '',
  streams: [],
  startTime: null,
  endTime: null,
  timeRange: 10,
};

const reducer = (state, action) => {
  let ref = Object.assign({}, state);
  if (action.type) ref.type = action.type;
  if (action.isLoading !== undefined) ref.isLoading = action.isLoading;
  switch (ref.type) {
    case tabName.topic:
      if (action.topicName) ref.topicName = action.topicName;
      if (action.topicLimit) ref.topicLimit = action.topicLimit;
      if (!isEmpty(action.topicData)) ref.topicData = action.topicData;
      return ref;
    case tabName.log:
      if (action.service) ref.service = action.service;
      if (action.logs) ref.logs = action.logs;
      if (action.hostname) ref.hostname = action.hostname;
      if (action.hostLog) ref.hostLog = action.hostLog;
      if (action.stream) ref.stream = action.stream;
      if (action.streams) ref.streams = action.streams;
      if (action.startTime) ref.startTime = action.startTime;
      if (action.endTime) ref.endTime = action.endTime;
      if (action.timeRange) ref.timeRange = action.timeRange;
      return ref;
    default:
  }
};

const DevToolDialog = () => {
  const { pipelineName } = useParams();
  const { workspace, data: topics } = useTopicState();
  const { fetchTopics } = useTopicActions();

  const [tabIndex, setTabIndex] = useState('topics');
  const { isOpen, close: closeDialog } = useDevToolDialog();

  const [data, setDataDispatch] = useReducer(reducer, initialState);
  const devEl = useRef(null);

  const fetchTopicData = useCallback(
    async (topicLimit = 10) => {
      setDataDispatch({ isLoading: true });
      const response = await inspectApi.getTopicData({
        name: data.topicName,
        group: workspace.settings.name,
        limit: topicLimit,
        timeout: 5000,
      });

      if (response) {
        const result = response.messages.map(message => {
          // we don't need the "tags" field in the topic data
          if (message.value) delete message.value.tags;
          return message;
        });

        setDataDispatch({ topicData: result });
      }
      setDataDispatch({ isLoading: false });
    },
    [data.topicName, workspace],
  );

  useEffect(() => {
    if (isEmpty(data.topicName) || isEmpty(workspace)) return;
    fetchTopicData();
  }, [data.topicName, workspace, fetchTopicData]);

  const fetchLogs = useCallback(async () => {
    let response = {};
    setDataDispatch({ isLoading: true });
    switch (data.service) {
      case 'configurator':
        response = await logApi.getConfiguratorLog();
        break;
      case 'zookeeper':
        const bkInfo = await brokerApi.get(workspace.settings.brokerClusterKey);
        response = await logApi.getZookeeperLog(
          bkInfo.settings.zookeeperClusterKey,
        );
        break;
      case 'broker':
        response = await logApi.getBrokerLog(
          workspace.settings.brokerClusterKey,
        );
        break;
      case 'worker':
        response = await logApi.getWorkerLog({
          name: workspace.settings.name,
          group: workspace.settings.group,
        });
        break;
      case 'stream':
        if (!isEmpty(data.stream) && !isEmpty(pipelineName)) {
          response = await logApi.getStreamLog({
            name: data.stream,
            group: workspace.settings.name + pipelineName,
          });
        }
        break;
      default:
    }
    if (!isEmpty(response)) {
      setDataDispatch({ logs: response.logs });
      setDataDispatch({ hostname: response.logs[0].hostname });
      const log = response.logs.find(
        log => log.hostname === response.logs[0].hostname,
      );
      if (log) {
        setDataDispatch({ hostLog: log.value.split('\n') });
      }
    }
    setDataDispatch({ isLoading: false });
  }, [data.service, data.stream, workspace, pipelineName]);

  const fetchStreams = useCallback(async () => {
    if (!isEmpty(workspace) && !isEmpty(pipelineName)) {
      const streamInfos = await streamApi.getAll({
        group: workspace.settings.name + pipelineName,
      });
      setDataDispatch({ streams: streamInfos.map(info => info.settings.name) });
    }
  }, [workspace, pipelineName]);

  useEffect(() => {
    if (!isEmpty(data.service)) {
      fetchLogs();
      if (data.service === 'stream') {
        fetchStreams();
      }
    }
  }, [data.service, fetchLogs, fetchStreams]);

  useEffect(() => {
    if (isEmpty(workspace)) return;
    if (tabIndex === 'topics') {
      fetchTopics(workspace.settings.name);
    }
  }, [fetchTopics, workspace, tabIndex]);

  const handleTabChange = (event, currentTab) => {
    if (workspace && currentTab === tabName.topic)
      fetchTopics(workspace.settings.name);

    setTabIndex(currentTab);
    setDataDispatch({ type: currentTab });
  };

  return (
    <StyledDevTool className={isOpen ? '' : 'is-close'} ref={devEl}>
      <Header
        tabIndex={tabIndex}
        topics={topics}
        handleTabChange={handleTabChange}
        closeDialog={closeDialog}
        setDataDispatch={setDataDispatch}
        data={data}
        fetchTopicData={fetchTopicData}
        fetchLogs={fetchLogs}
      />
      <Body tabIndex={tabIndex} data={data} dialogEl={devEl.current}></Body>
    </StyledDevTool>
  );
};

export default DevToolDialog;
