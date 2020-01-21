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
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { isEmpty } from 'lodash';
import { useLocation, useParams } from 'react-router-dom';
import { CellMeasurerCache } from 'react-virtualized/dist/commonjs/CellMeasurer';
import { Typography } from '@material-ui/core';

import { KIND } from 'const';
import { usePrevious } from 'utils/hooks';
import * as context from 'context';
import { TIME_GROUP } from 'context/log/const';
import { TAB } from 'context/devTool/const';
import { ViewTopic, ViewLog } from './View';

// the react-virtualized <List> cached row style
const cache = new CellMeasurerCache({
  defaultHeight: 20,
  fixedWidth: true,
});

const WindowDiv = styled.div`
  height: 100vh;
`;

const DataWindow = () => {
  const location = useLocation();
  const { workspaceName, pipelineName } = useParams();
  const { setWorkspaceName, setPipelineName } = context.useApp();
  const {
    isFetching: isFetchingTopic,
    lastUpdated: lastUpdatedTopic,
  } = context.useTopicDataState();
  const {
    isFetching: isFetchingLog,
    lastUpdated: lastUpdatedLog,
  } = context.useLogState();
  const { data: topics } = context.useTopicState();
  const topicActions = context.useTopicDataActions();
  const logActions = context.useLogActions();
  const {
    currentZookeeper,
    currentBroker,
    currentWorker,
  } = context.useWorkspace();

  const searchParams = new URLSearchParams(location.search);

  const type = searchParams.get('type') || '';

  // topics tab query parameter
  const topicName = searchParams.get('topicName') || '';
  const topicLimit = searchParams.get('topicLimit') || 10;
  // logs tab query parameter
  const logType = searchParams.get('logType') || '';
  const hostname = searchParams.get('hostname') || '';
  const streamName = searchParams.get('streamName') || '';
  const timeGroup = searchParams.get('timeGroup') || '';
  const timeRange = searchParams.get('timeRange') || '';
  const startTime = searchParams.get('startTime') || '';
  const endTime = searchParams.get('endTime') || '';

  const prevWorkspaceName = usePrevious(workspaceName);
  const prevPipelineName = usePrevious(pipelineName);

  React.useEffect(() => {
    function handleResize() {
      // when window resize, we force re-render the log data height
      // give it a little delay to avoid performance issue
      setTimeout(cache.clearAll(), 500);
    }

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []); // Empty array ensures that effect is only run on mount and unmount

  React.useEffect(() => {
    if (workspaceName && workspaceName !== prevWorkspaceName) {
      setWorkspaceName(workspaceName);
    }
  }, [prevWorkspaceName, setWorkspaceName, workspaceName]);

  React.useEffect(() => {
    if (pipelineName && pipelineName !== prevPipelineName) {
      setPipelineName(pipelineName);
    }
  }, [pipelineName, prevPipelineName, setPipelineName]);

  React.useEffect(() => {
    if (!topicActions || lastUpdatedTopic) return;

    // do nothing if the query parameters not completed
    if (
      isEmpty(topics) ||
      isEmpty(topicName) ||
      isEmpty(topicLimit) ||
      isFetchingTopic
    )
      return;

    topicActions.fetchTopicData({
      name: topicName,
      limit: topicLimit,
    });
  }, [
    lastUpdatedTopic,
    isFetchingTopic,
    topicActions,
    topicLimit,
    topicName,
    topics,
  ]);

  React.useEffect(() => {
    if (!logActions || lastUpdatedLog) return;

    // do nothing if the query parameters not completed
    if (
      isFetchingLog ||
      isEmpty(logType) ||
      isEmpty(hostname) ||
      isEmpty(timeGroup)
    )
      return;

    // do nothing if logType was configurator and the context not ready
    if (
      logType !== KIND.configurator &&
      (!currentZookeeper || !currentBroker || !currentWorker)
    )
      return;

    const getTimeSeconds = () => {
      if (timeGroup === TIME_GROUP.latest) {
        // timeRange uses minute units
        return timeRange * 60;
      } else {
        return Math.ceil(
          moment.duration(moment(endTime).diff(moment(startTime))).asSeconds(),
        );
      }
    };

    const setHostName = () => {
      logActions.setHostName(hostname);
    };

    switch (logType) {
      case KIND.configurator:
        logActions.fetchConfiguratorLog(getTimeSeconds()).then(setHostName);
        break;
      case KIND.zookeeper:
        logActions.fetchZookeeperLog(getTimeSeconds()).then(setHostName);
        break;
      case KIND.broker:
        logActions.fetchBrokerLog(getTimeSeconds().then(setHostName));
        break;
      case KIND.worker:
        logActions.fetchWorkerLog(getTimeSeconds().then(setHostName));
        break;
      case KIND.stream:
        if (isEmpty(streamName)) return;
        logActions
          .fetchStreamLog({
            name: streamName,
            sinceSeconds: getTimeSeconds(),
          })
          .then(setHostName);
        break;
      default:
    }
  }, [
    lastUpdatedLog,
    endTime,
    hostname,
    isFetchingLog,
    logActions,
    logType,
    startTime,
    streamName,
    timeGroup,
    timeRange,
    currentZookeeper,
    currentBroker,
    currentWorker,
  ]);

  if (!location.search) {
    return <Typography>There is nothing to show</Typography>;
  }

  switch (type) {
    case TAB.topic:
      return <ViewTopic />;
    case TAB.log:
      return (
        <WindowDiv>
          <ViewLog />
        </WindowDiv>
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
