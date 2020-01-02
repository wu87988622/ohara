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

import React, { useEffect, useState, useCallback, useReducer } from 'react';
import moment from 'moment';
import { isEmpty, get } from 'lodash';
import { CellMeasurerCache } from 'react-virtualized/dist/commonjs/CellMeasurer';
import * as inspectApi from 'api/inspectApi';
import * as logApi from 'api/logApi';
import * as streamApi from 'api/streamApi';
import Header from './Header';
import Body from './Body';
import StatusBar from './StatusBar';
import * as context from 'context';
import { usePrevious } from 'utils/hooks';
import { StyledDevTool } from './DevToolDialogStyles';
import { hashByGroupAndName } from 'utils/sha';

export const tabName = {
  topic: 'topics',
  log: 'logs',
};

const initialState = {
  type: tabName.topic,
  isLoading: false,

  /* topics tab */
  topicName: '',
  topicLimit: 10,
  topicData: [],

  /* logs tab */
  service: '',
  streams: [],
  hosts: [],
  hostname: '',
  stream: '',
  hostLog: [],
  timeGroup: 'latest',
  timeRange: 10,
  startTime: '',
  endTime: '',
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
      if (action.streams) ref.streams = action.streams;
      if (action.hosts) ref.hosts = action.hosts;
      if (action.hostname) ref.hostname = action.hostname;
      if (action.hostLog) ref.hostLog = action.hostLog;
      if (action.stream) ref.stream = action.stream;
      if (action.timeGroup) ref.timeGroup = action.timeGroup;
      if (action.timeRange) ref.timeRange = action.timeRange;
      if (action.startTime) ref.startTime = action.startTime;
      if (action.endTime) ref.endTime = action.endTime;
      return ref;
    default:
  }
};

// the react-virtualized <List> cached row style
const listCache = new CellMeasurerCache({
  defaultHeight: 20,
  fixedWidth: true,
});

const DevToolDialog = () => {
  const [tabIndex, setTabIndex] = useState('topics');
  const [data, setDataDispatch] = useReducer(reducer, initialState);

  const { pipelineName, workspaceName } = context.useApp();
  const { data: topics } = context.useTopicState();
  const { currentWorkspace } = context.useWorkspace();
  const { isOpen, close: closeDialog } = context.useDevToolDialog();
  const { selectedCell } = context.usePipelineState();
  const { setSelectedCell } = context.usePipelineActions();

  const getService = classType => {
    if (classType === 'source' || classType === 'sink') return 'worker';
    if (classType === 'topic') return 'broker';
    if (classType === 'stream') return 'stream';
  };

  useEffect(() => {
    // The selected cell could be other connector types do a quick check here
    if (!selectedCell || !isOpen) return;

    const isTopic = get(selectedCell, 'classType', null) === 'topic';

    if (isTopic) {
      if (data.type === 'logs') {
        return setDataDispatch({
          topicName: selectedCell.name,
          service: 'broker',
        });
      }
      return setDataDispatch({ topicName: selectedCell.name });
    }

    setTabIndex(tabName.log);
    const service = getService(selectedCell.classType);
    setDataDispatch({ type: tabName.log, service });
  }, [data.type, isOpen, selectedCell, topics]);

  useEffect(() => {
    function handleResize() {
      // when window resize, we force re-render the log data height
      // give it a little delay to avoid performance issue
      setTimeout(listCache.clearAll(), 500);
    }

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []); // Empty array ensures that effect is only run on mount and unmount

  const fetchTopicData = useCallback(
    async (topicLimit = 10) => {
      setDataDispatch({ isLoading: true });

      // We're displaying private topic names by using their displayNames
      // However, they're stored in the backend by their names, so we need to
      // A quick swap here.
      const privateTopic =
        // Only private topics are able to use capital letters as name
        data.topicName.startsWith('T') &&
        topics.find(topic => topic.tags.displayName === data.topicName);

      const response = await inspectApi.getTopicData({
        name: privateTopic ? privateTopic.name : data.topicName,
        group: hashByGroupAndName('workspace', workspaceName),
        limit: topicLimit,
        timeout: 5000,
      });

      if (!response.errors) {
        const result = response.data.messages.map(message => {
          // we don't need the "tags" field in the topic data
          if (message.value) delete message.value.tags;
          return message;
        });

        setDataDispatch({ topicData: result });
      }
      setDataDispatch({ isLoading: false });
    },
    [data.topicName, topics, workspaceName],
  );

  const prevTopicName = usePrevious(data.topicName);
  useEffect(() => {
    if (isEmpty(data.topicName) || isEmpty(currentWorkspace)) return;
    if (prevTopicName === data.topicName) return;
    if (data.isLoading) return;

    fetchTopicData();
  }, [
    currentWorkspace,
    data.isLoading,
    data.topicName,
    fetchTopicData,
    prevTopicName,
  ]);

  const fetchLogs = useCallback(
    async (timeSeconds = 600, hostname = '') => {
      let response;
      setDataDispatch({ isLoading: true });
      switch (data.service) {
        case 'configurator':
          response = await logApi.getConfiguratorLog({
            sinceSeconds: timeSeconds,
          });
          break;
        case 'zookeeper':
          response = await logApi.getZookeeperLog({
            name: workspaceName,
            group: 'zookeeper',
            sinceSeconds: timeSeconds,
          });
          break;
        case 'broker':
          response = await logApi.getBrokerLog({
            name: workspaceName,
            group: 'broker',
            sinceSeconds: timeSeconds,
          });

          break;
        case 'worker':
          response = await logApi.getWorkerLog({
            name: workspaceName,
            group: 'worker',
            sinceSeconds: timeSeconds,
          });
          break;
        case 'stream':
          const pipelineGroup = hashByGroupAndName('workspace', workspaceName);

          if (data.stream && pipelineName) {
            response = await logApi.getStreamLog({
              name: data.stream,
              group: hashByGroupAndName(pipelineGroup, pipelineName),
              sinceSeconds: timeSeconds,
            });
          }
          break;
        default:
      }

      if (response && !response.errors) {
        const logResponse = response.data;
        setDataDispatch({ hosts: logResponse.logs.map(log => log.hostname) });

        let logData = null;
        if (isEmpty(hostname)) {
          if (logResponse.logs.length > 0) {
            setDataDispatch({ hostname: logResponse.logs[0].hostname });
            logData = logResponse.logs.find(
              log => log.hostname === logResponse.logs[0].hostname,
            );
          }
        } else {
          logData = logResponse.logs.find(log => log.hostname === hostname);
        }

        if (logData) {
          setDataDispatch({ hostLog: logData.value.split('\n') });
          // when log data updated, we force re-render the log data height
          listCache.clearAll();
        } else {
          setDataDispatch({ hostLog: [] });
        }
      }

      setDataDispatch({ isLoading: false });
    },
    [data.service, data.stream, pipelineName, workspaceName],
  );

  const fetchStreams = useCallback(async () => {
    if (!isEmpty(currentWorkspace) && !isEmpty(pipelineName)) {
      const pipelineGroup = hashByGroupAndName('workspace', workspaceName);

      const streamInfos = await streamApi.getAll({
        group: hashByGroupAndName(pipelineGroup, pipelineName),
      });
      if (!streamInfos.errors) {
        setDataDispatch({
          streams: streamInfos.data.map(info => info.name),
        });
      }
    }
  }, [currentWorkspace, pipelineName, workspaceName]);

  useEffect(() => {
    if (isEmpty(currentWorkspace)) return;
    if (!isEmpty(data.service)) {
      fetchLogs();
      if (data.service === 'stream') {
        fetchStreams();
      }
    }
  }, [currentWorkspace, data.service, data.stream, fetchLogs, fetchStreams]);

  const handleTabChange = (event, currentTab) => {
    setSelectedCell(null);
    setTabIndex(currentTab);
    setDataDispatch({ type: currentTab });
  };

  const getStatusText = () => {
    if (tabIndex === tabName.topic) {
      if (isEmpty(data.topicData)) {
        return 'No topic data';
      } else {
        return `${data.topicData.length} rows per query`;
      }
    } else {
      if (isEmpty(data.hostLog)) {
        return 'No log data';
      } else {
        switch (data.timeGroup) {
          case 'latest':
            return `Latest ${data.timeRange} minutes`;
          case 'customize':
            return `Customize from ${moment(data.startTime).format(
              'YYYY/MM/DD hh:mm',
            )} to ${moment(data.endTime).format('YYYY/MM/DD hh:mm')}`;
          default:
            return 'Unexpected time format';
        }
      }
    }
  };

  return (
    <StyledDevTool className={isOpen ? '' : 'is-close'}>
      <Header
        tabIndex={tabIndex}
        topics={topics}
        handleTabChange={handleTabChange}
        closeDialog={closeDialog}
        setDataDispatch={setDataDispatch}
        data={data}
        fetchTopicData={fetchTopicData}
        fetchLogs={fetchLogs}
        pipelineName={pipelineName}
      />
      <Body tabIndex={tabIndex} data={data} cache={listCache}></Body>
      <StatusBar tabIndex={tabIndex} statusText={getStatusText()} />
    </StyledDevTool>
  );
};

export default DevToolDialog;
