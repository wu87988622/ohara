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
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { useLocation, useParams } from 'react-router-dom';
import { CellMeasurerCache } from 'react-virtualized/dist/commonjs/CellMeasurer';
import { Typography } from '@material-ui/core';

import { DevToolTabName } from 'const';
import { ViewTopic, ViewLog } from './View';
import * as hooks from 'hooks';

// the react-virtualized <List> cached row style
const cache = new CellMeasurerCache({
  defaultHeight: 20,
  fixedWidth: true,
});

const WindowDiv = styled.div`
  height: 100vh;
  overflow: auto;
`;

const DataWindow = () => {
  const location = useLocation();
  const { workspaceName, pipelineName } = useParams();
  const isWorkspaceReady = hooks.useIsWorkspaceReady();
  const isTopicDataQueried = hooks.useIsDevToolTopicDataQueried();
  const isLogQueried = hooks.useIsDevToolLogQueried();
  const setTopicQueryParams = hooks.useSetDevToolTopicQueryParams();
  const setLogQueryParams = hooks.useSetDevToolLogQueryParams();

  const searchParams = new URLSearchParams(location.search);

  const type = searchParams.get('type') || '';

  // topics tab query parameter
  const topicName = searchParams.get('topicName') || '';
  const topicLimit = searchParams.get('topicLimit') || 10;
  // logs tab query parameter
  const logType = searchParams.get('logType') || '';
  const hostName = searchParams.get('hostname') || '';
  const streamName = searchParams.get('streamName') || '';
  const timeGroup = searchParams.get('timeGroup') || '';
  const timeRange = searchParams.get('timeRange') || '';
  const startTime = searchParams.get('startTime') || '';
  const endTime = searchParams.get('endTime') || '';

  hooks.useInitializeApp(workspaceName, pipelineName);

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
    if (isWorkspaceReady && !isTopicDataQueried) {
      setTopicQueryParams({ name: topicName, limit: topicLimit });
    }
  }, [
    isTopicDataQueried,
    isWorkspaceReady,
    setTopicQueryParams,
    topicLimit,
    topicName,
  ]);

  React.useEffect(() => {
    if (isWorkspaceReady && !isLogQueried) {
      setLogQueryParams({
        logType,
        hostName,
        streamName,
        timeGroup,
        timeRange,
        startTime,
        endTime,
      });
    }
  }, [
    endTime,
    hostName,
    isLogQueried,
    isWorkspaceReady,
    logType,
    setLogQueryParams,
    startTime,
    streamName,
    timeGroup,
    timeRange,
  ]);

  if (!location.search) {
    return <Typography>There is nothing to show</Typography>;
  }

  switch (type) {
    case DevToolTabName.TOPIC:
      return (
        <WindowDiv data-testid="data-window">
          <ViewTopic />
        </WindowDiv>
      );
    case DevToolTabName.LOG:
      return (
        <WindowDiv data-testid="data-window">
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
