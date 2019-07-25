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
import ReactTooltip from 'react-tooltip';
import toastr from 'toastr';

import * as connectorApi from 'api/connectorApi';
import * as streamApi from 'api/streamApi';
import { Box } from 'components/common/Layout';
import { Heading3, OperateWrapper } from './styles';
import { getConnectors } from '../pipelineUtils/commonUtils';

const Operate = props => {
  const {
    workerClusterName,
    fetchPipeline,
    pipelineConnectors,
    pipelineName,
    updateHasRunningServices,
  } = props;

  const makeRequest = (action, connectors) => {
    const { sources, sinks, streams } = getConnectors(connectors);
    const _connectors = sources.concat(sinks);

    let connectorPromises = [];
    let streamsPromises = [];

    if (action === 'start') {
      connectorPromises = _connectors.map(connector =>
        connectorApi.startConnector(connector),
      );

      streamsPromises = streams.map(stream => streamApi.stopStreamApp(stream));
    } else {
      connectorPromises = _connectors.map(connector =>
        connectorApi.stopConnector(connector),
      );

      streamsPromises = streams.map(stream => streamApi.stopStreamApp(stream));
    }

    return Promise.all([...connectorPromises, ...streamsPromises]).then(
      result => result,
    );
  };

  const handleActionClick = async action => {
    let responses = null;

    if (action === 'start') {
      responses = await makeRequest(action, pipelineConnectors);
    } else {
      responses = await makeRequest(action, pipelineConnectors);
    }

    const hasError = responses.some(response => !response.data.isSuccess);

    if (!hasError) {
      toastr.success(`Pipeline has been successfully ${action}!`);

      const hasRunningServices = action === 'start';
      updateHasRunningServices(hasRunningServices);
      await fetchPipeline(pipelineName);
    }
  };

  return (
    <Box>
      <OperateWrapper>
        <div className="actions">
          <Heading3>Operate</Heading3>
          <ReactTooltip />

          <div className="action-btns">
            <button
              className="start-btn"
              data-tip="Start pipeline"
              onClick={() => handleActionClick('start')}
              data-testid="start-btn"
            >
              <i className="far fa-play-circle" />
            </button>
            <button
              className="stop-btn"
              data-tip="Stop pipeline"
              onClick={() => handleActionClick('stop')}
              data-testid="stop-btn"
            >
              <i className="far fa-stop-circle" />
            </button>
          </div>
        </div>
        <span className="cluster-name">
          This pipeline is running on: {workerClusterName}
        </span>
      </OperateWrapper>
    </Box>
  );
};

Operate.propTypes = {
  workerClusterName: PropTypes.string.isRequired,
  fetchPipeline: PropTypes.func.isRequired,
  pipelineConnectors: PropTypes.arrayOf(PropTypes.object),
  pipelineName: PropTypes.string.isRequired,
  updateHasRunningServices: PropTypes.func.isRequired,
};

export default Operate;
