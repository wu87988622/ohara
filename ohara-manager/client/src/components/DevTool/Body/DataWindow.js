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
import PropTypes from 'prop-types';
import { isEmpty } from 'lodash';
import { useLocation, useParams } from 'react-router-dom';
import { CellMeasurerCache } from 'react-virtualized/dist/commonjs/CellMeasurer';
import { WindowScroller } from 'react-virtualized/dist/commonjs/WindowScroller';

import * as inspectApi from 'api/inspectApi';
import * as logApi from 'api/logApi';
import DataTable from './DataTable';
import { tabName } from '../DevToolDialog';
import { hashByGroupAndName } from 'utils/sha';
import { useSnackbar } from 'context';

// the react-virtualized <List> cached row style
const cache = new CellMeasurerCache({
  defaultHeight: 20,
  fixedWidth: true,
});

const DataWindow = () => {
  const location = useLocation();
  const { workspaceName, pipelineName } = useParams();

  const searchParams = new URLSearchParams(location.search);
  const [isLoadingData, setIsLoadingData] = useState(false);
  const [topicResult, setTopicResult] = useState([]);
  const [logData, setLogData] = useState([]);
  const showMessage = useSnackbar();

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

  const fetchTopicData = useCallback(async () => {
    if (type !== tabName.topic) return;
    setIsLoadingData(true);
    let data = [];
    const response = await inspectApi.getTopicData({
      name: topicName,
      group: hashByGroupAndName('workspace', workspaceName),
      limit: topicLimit,
      timeout: topicTimeout,
    });

    if (!response.errors) {
      data = response.data.messages.map(message => {
        // we don't need the "tags" field in the topic data
        if (message.value) delete message.value.tags;
        return message;
      });
    }

    setTopicResult(data);
    setIsLoadingData(false);
  }, [type, topicName, workspaceName, topicLimit, topicTimeout]);

  useEffect(() => {
    if (!topicName) return;
    fetchTopicData();
  }, [topicName, fetchTopicData]);

  useEffect(() => {
    if (isEmpty(service)) return;

    const fetchLogs = async () => {
      let response;
      setIsLoadingData(true);
      switch (service) {
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
          if (!isEmpty(stream) && !isEmpty(pipelineName)) {
            response = await logApi.getStreamLog({
              name: stream,
              group: hashByGroupAndName(pipelineGroup, pipelineName),
              sinceSeconds: timeSeconds,
            });
          }
          break;
        default:
      }

      showMessage(response.title);
      setIsLoadingData(false);

      if (response && !response.errors) {
        const result = response.data.logs
          // the hostname log should be unique, it is OK to "filter" the result
          .filter(log => log.hostname === hostname)
          .map(log => log.value.split('\n'));

        if (!isEmpty(result)) setLogData(result[0]);
        return;
      }

      response.errors && showMessage(response.title);
    };

    fetchLogs();
  }, [
    hostname,
    pipelineName,
    service,
    showMessage,
    stream,
    timeSeconds,
    workspaceName,
  ]);

  // we don't generate the data view if no query parameters existed
  if (!workspaceName || !pipelineName || !location.search) return null;

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
