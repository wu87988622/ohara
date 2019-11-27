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
import React, { useState, useCallback, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import PropTypes from 'prop-types';

import { CellMeasurerCache } from 'react-virtualized/dist/commonjs/CellMeasurer';
import { WindowScroller } from 'react-virtualized/dist/commonjs/WindowScroller';

import { useWorkspace } from 'context';
import DataTable from './DataTable';
import * as inspectApi from 'api/inspectApi';
import * as logApi from 'api/logApi';
import * as brokerApi from 'api/brokerApi';

import { tabName } from '../DevToolDialog';

// the react-virtualized <List> cached row style
const cache = new CellMeasurerCache({
  defaultHeight: 20,
  fixedWidth: true,
});

const DataWindow = props => {
  const { location } = props;
  const searchParams = new URLSearchParams(location.search);
  const { workspaceName } = useParams();
  const { findByWorkspaceName } = useWorkspace();

  const [isLoadingData, setIsLoadingData] = useState(false);
  const [topicResult, setTopicResult] = useState([]);
  const [logData, setLogData] = useState([]);

  const currentWorkspace = findByWorkspaceName(workspaceName);

  const type = searchParams.get('type') || '';

  // topics tab query parameter
  const topicName = searchParams.get('topic') || '';
  const topicLimit = searchParams.get('limit') || 10;
  const topicTimeout = searchParams.get('timeout') || 5000;
  // logs tab query parameter
  const service = searchParams.get('service') || '';
  const hostname = searchParams.get('hostname') || '';
  const timeSeconds = searchParams.get('timeSeconds') || '';
  const stream = searchParams.get('stream') || '';
  const pipelineName = searchParams.get('pipelineName') || '';

  const fetchTopicData = useCallback(async () => {
    if (type !== tabName.topic) return;
    setIsLoadingData(true);
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

    setTopicResult(data);
    setIsLoadingData(false);
  }, [type, topicName, topicLimit, topicTimeout, currentWorkspace]);

  useEffect(() => {
    if (!topicName || !currentWorkspace) return;
    fetchTopicData();
  }, [topicName, currentWorkspace, fetchTopicData]);

  const fetchLogs = useCallback(async () => {
    let response = {};
    setIsLoadingData(true);
    switch (service) {
      case 'configurator':
        response = await logApi.getConfiguratorLog({
          sinceSeconds: timeSeconds,
        });
        break;
      case 'zookeeper':
        const bkInfo = await brokerApi.get(
          currentWorkspace.settings.brokerClusterKey,
        );
        response = await logApi.getZookeeperLog({
          name: bkInfo.settings.zookeeperClusterKey.name,
          group: bkInfo.settings.zookeeperClusterKey.group,
          sinceSeconds: timeSeconds,
        });
        break;
      case 'broker':
        response = await logApi.getBrokerLog({
          name: currentWorkspace.settings.brokerClusterKey.name,
          group: currentWorkspace.settings.brokerClusterKey.group,
          sinceSeconds: timeSeconds,
        });
        break;
      case 'worker':
        response = await logApi.getWorkerLog({
          name: currentWorkspace.settings.name,
          group: currentWorkspace.settings.group,
          sinceSeconds: timeSeconds,
        });
        break;
      case 'stream':
        if (!isEmpty(stream) && !isEmpty(pipelineName)) {
          response = await logApi.getStreamLog({
            name: stream,
            group: currentWorkspace.settings.name + pipelineName,
            sinceSeconds: timeSeconds,
          });
        }
        break;
      default:
    }
    if (!isEmpty(response)) {
      const result = response.logs
        // the hostname log should be unique, it is OK to "filter" the result
        .filter(log => log.hostname === hostname)
        .map(log => log.value.split('\n'));
      if (!isEmpty(result)) {
        setLogData(result[0]);
      }
    }
    setIsLoadingData(false);
  }, [service, hostname, stream, timeSeconds, currentWorkspace, pipelineName]);

  useEffect(() => {
    if (isEmpty(service) || !currentWorkspace) return;

    fetchLogs();
  }, [service, currentWorkspace, fetchLogs]);

  // we don't generate the data view if no query parameters existed
  if (!currentWorkspace || !location.search) return null;

  switch (type) {
    case tabName.topic:
      return (
        <DataTable
          data={{
            topicData: topicResult,
            isLoading: isLoadingData,
          }}
          type={tabName.topic}
        />
      );
    case tabName.log:
      return (
        <WindowScroller>
          {({ height, isScrolling, onChildScroll, scrollTop }) => (
            <DataTable
              data={{
                hostLog: logData,
                isLoading: isLoadingData,
              }}
              type={tabName.log}
              cache={cache}
              windowOpts={{
                windowHeight: height,
                windowIsScrolling: isScrolling,
                windowOnScroll: onChildScroll,
                windowScrollTop: scrollTop,
              }}
            />
          )}
        </WindowScroller>
      );

    default:
      return null;
  }
};

DataWindow.propTypes = {
  location: PropTypes.shape({
    search: PropTypes.string,
  }).isRequired,
};

export default DataWindow;
