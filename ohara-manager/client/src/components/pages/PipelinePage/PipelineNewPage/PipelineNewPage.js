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
import DocumentTitle from 'react-document-title';
import toastr from 'toastr';
import PropTypes from 'prop-types';
import ReactTooltip from 'react-tooltip';
import { Route, Prompt } from 'react-router-dom';
import { get, isEmpty } from 'lodash';

import * as MESSAGES from 'constants/messages';
import * as PIPELINES from 'constants/pipelines';
import * as pipelineApi from 'api/pipelineApi';
import * as connectorApi from 'api/connectorApi';
import * as streamApi from 'api/streamApi';
import * as topicApi from 'api/topicApi';
import * as workerApi from 'api/workerApi';
import * as Connectors from '../Connectors';
import * as utils from './pipelineNewPageUtils';
import PipelineToolbar from '../PipelineToolbar';
import PipelineGraph from '../PipelineGraph';
import { Box } from 'components/common/Layout';
import { getConnectors } from '../pipelineUtils/commonUtils';
import { PIPELINE_NEW, PIPELINE_EDIT } from 'constants/documentTitles';

import { Wrapper, Main, Sidebar, Heading2, Heading3, Operate } from './styles';

class PipelineNewPage extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      params: PropTypes.object.isRequired,
    }).isRequired,
  };

  state = {
    topics: [],
    currentTopic: null,
    graph: [],
    isLoading: true,
    isUpdating: false,
    hasChanges: false,
    runningConnectors: 0,
    pipeline: {},
    pipelineTopics: [],
    connectors: [],
    brokerClusterName: '',
  };

  componentDidMount() {
    this.fetchData();
  }

  fetchData = async () => {
    await this.fetchPipeline(); // we need workerClusterName from this request for the following fetchWorker() request
    await this.fetchWorker();
    this.fetchTopics();
  };

  fetchTopics = async () => {
    const res = await topicApi.fetchTopics();
    this.setState(() => ({ isLoading: false }));

    const topics = get(res, 'data.result', null);

    if (!isEmpty(topics)) {
      const { brokerClusterName } = this.state;
      const topicsUnderBrokerCluster = topics.filter(
        topic => topic.brokerClusterName === brokerClusterName,
      );

      if (topicsUnderBrokerCluster) {
        this.setState({
          topics: topicsUnderBrokerCluster,
          currentTopic: topicsUnderBrokerCluster[0],
        });
      }
    }
  };

  fetchPipeline = async () => {
    const { match } = this.props;
    const pipelineName = get(match, 'params.pipelineName', null);

    if (pipelineName) {
      const res = await pipelineApi.fetchPipeline(pipelineName);
      const pipeline = get(res, 'data.result', null);

      if (pipeline) {
        const updatedPipeline = utils.addPipelineStatus(pipeline);
        const { topics: pipelineTopics = [] } = getConnectors(
          updatedPipeline.objects,
        );

        this.setState({ pipeline: updatedPipeline, pipelineTopics }, () => {
          this.loadGraph(this.state.pipeline);
        });
      }
    }
  };

  fetchWorker = async () => {
    const { workerClusterName: name } = this.state.pipeline;
    const res = await workerApi.fetchWorker(name);
    const worker = get(res, 'data.result', null);

    if (worker) {
      this.setState({
        connectors: worker.connectors,
        brokerClusterName: worker.brokerClusterName,
      });
    }
  };

  updateGraph = async params => {
    this.setState(({ graph }) => {
      return {
        graph: utils.updateGraph({
          graph,
          ...params,
        }),
      };
    });

    await this.updatePipeline({ ...params });
  };

  loadGraph = pipeline => {
    this.setState(() => {
      return { graph: utils.loadGraph(pipeline) };
    });
  };

  resetGraph = () => {
    this.setState(({ graph }) => {
      const update = graph.map(g => {
        return { ...g, isActive: false };
      });

      return {
        graph: update,
      };
    });
  };

  refreshGraph = () => {
    const { pipelineName } = this.props.match.params;
    if (pipelineName) {
      this.fetchPipeline(pipelineName);
    }
  };

  updateHasChanges = update => {
    this.setState({ hasChanges: update });
  };

  updateCurrentTopic = currentTopic => {
    this.setState({ currentTopic });
  };

  resetCurrentTopic = () => {
    this.setState(({ topics }) => ({ currentTopic: topics[0] }));
  };

  updatePipeline = async (update = {}) => {
    const { pipeline } = this.state;
    const { name, status } = pipeline;
    const params = utils.updatePipelineParams({ pipeline, ...update });

    this.setState({ isUpdating: true }, async () => {
      const res = await pipelineApi.updatePipeline({ name, params });

      this.setState({ isUpdating: false });
      const updatedPipelines = get(res, 'data.result', null);

      if (!isEmpty(updatedPipelines)) {
        const { topics: pipelineTopics } = getConnectors(
          updatedPipelines.objects,
        );

        // Keep the pipeline status since that's not stored on the configurator
        this.setState({
          pipeline: { ...updatedPipelines, status },
          pipelineTopics,
        });
      }
    });
  };

  checkPipelineStatus = async () => {
    const pipelineName = get(this.props.match, 'params.pipelineName', null);
    await this.fetchPipeline(pipelineName);

    const { status, objects: connectors } = this.state.pipeline;

    if (!status) {
      toastr.error(MESSAGES.CANNOT_START_PIPELINE_ERROR);
    }

    return { connectors, status };
  };

  handlePipelineStartClick = async () => {
    const { connectors, status } = await this.checkPipelineStatus();

    if (status) {
      const res = await this.startConnectors(connectors);
      const isSuccess = res.filter(r => r.data.isSuccess);
      this.handleConnectorResponse(isSuccess, 'started');
    }
  };

  handlePipelineStopClick = async () => {
    const { connectors, status } = await this.checkPipelineStatus();

    if (status) {
      const res = await this.stopConnectors(connectors);
      const isSuccess = res.filter(r => r.data.isSuccess);
      this.handleConnectorResponse(isSuccess, 'stopped');
    }
  };

  updateRunningConnectors = (sources, sinks, streams) => {
    const runningConnectors = [...sources, ...sinks, ...streams].length;
    this.setState({ runningConnectors });
  };

  startConnectors = async connectors => {
    const { sources, sinks, streams } = getConnectors(connectors);
    this.updateRunningConnectors(sources, sinks, streams);

    const connectorPromises = [...sources, ...sinks].map(source =>
      connectorApi.startConnector(source),
    );
    const streamsPromises = streams.map(stream =>
      streamApi.startStreamApp(stream),
    );

    return Promise.all([...connectorPromises, ...streamsPromises]).then(
      result => result,
    );
  };

  stopConnectors = connectors => {
    const { sources, sinks, streams } = getConnectors(connectors);
    this.updateRunningConnectors(sources, sinks, streams);

    const connectorPromises = [...sources, ...sinks].map(sink =>
      connectorApi.stopConnector(sink),
    );
    const streamsPromises = streams.map(stream =>
      streamApi.stopStreamApp(stream),
    );

    return Promise.all([...connectorPromises, ...streamsPromises]).then(
      result => result,
    );
  };

  handleConnectorResponse = (isSuccess, action) => {
    if (isSuccess.length === this.state.runningConnectors) {
      toastr.success(`Pipeline has been successfully ${action}!`);
      let status = action === 'started' ? 'Running' : 'Stopped';

      this.setState(({ pipeline }) => {
        return {
          runningConnectors: 0,
          pipeline: {
            ...pipeline,
            status,
          },
        };
      });

      const pipelineName = get(this.props.match, 'params.pipelineName', null);
      this.fetchPipeline(pipelineName);
    } else {
      toastr.error(MESSAGES.CANNOT_START_PIPELINE_ERROR);
    }
  };

  getConnectorDefs = ({ connectors, type }) => {
    const getByClassName = connector => connector.className === type;
    const connector = connectors.find(getByClassName);

    return connector.definitions;
  };

  render() {
    const {
      isLoading,
      isUpdating,
      graph,
      topics,
      pipelineTopics,
      currentTopic,
      hasChanges,
      pipeline,
      connectors,
    } = this.state;

    if (isEmpty(pipeline) || isEmpty(connectors)) return null;

    const pipelineName = get(this, 'props.match.params.pipelineName', null);
    const {
      name: pipelineTitle,
      status: pipelineStatus,
      workerClusterName,
    } = pipeline;

    const isPipelineRunning = pipelineStatus === 'Running' ? true : false;

    const {
      jdbcSource,
      ftpSource,
      hdfsSink,
      ftpSink,
      customSource,
    } = PIPELINES.CONNECTOR_TYPES;

    const connectorProps = {
      loadGraph: this.loadGraph,
      updateGraph: this.updateGraph,
      refreshGraph: this.refreshGraph,
      updateHasChanges: this.updateHasChanges,
      pipelineTopics: pipelineTopics,
      globalTopics: topics,
      isPipelineRunning,
      pipeline,
      hasChanges,
      graph,
    };

    const routeBaseUrl = `/pipelines/(new|edit)`;

    return (
      <DocumentTitle title={pipelineName ? PIPELINE_EDIT : PIPELINE_NEW}>
        <>
          <Prompt
            message={location =>
              location.pathname.startsWith('/pipelines/new') ||
              location.pathname.startsWith('/pipelines/edit')
                ? true
                : MESSAGES.LEAVE_WITHOUT_SAVE
            }
            when={isUpdating}
          />
          <Wrapper>
            <PipelineToolbar
              {...this.props}
              updateGraph={this.updateGraph}
              graph={graph}
              hasChanges={hasChanges}
              topics={topics}
              currentTopic={currentTopic}
              isLoading={isLoading}
              resetCurrentTopic={this.resetCurrentTopic}
              updateCurrentTopic={this.updateCurrentTopic}
              workerClusterName={workerClusterName}
            />

            <Main>
              <PipelineGraph
                {...this.props}
                graph={graph}
                pipeline={pipeline}
                updateGraph={this.updateGraph}
                resetGraph={this.resetGraph}
              />

              <Sidebar>
                <Heading2>{pipelineTitle}</Heading2>

                <Box>
                  <Operate>
                    <div className="actions">
                      <Heading3>Operate</Heading3>
                      <ReactTooltip />

                      <div className="action-btns">
                        <button
                          className="start-btn"
                          data-tip="Start pipeline"
                          onClick={this.handlePipelineStartClick}
                          data-testid="start-btn"
                        >
                          <i className="far fa-play-circle" />
                        </button>
                        <button
                          className="stop-btn"
                          data-tip="Stop pipeline"
                          onClick={this.handlePipelineStopClick}
                          data-testid="stop-btn"
                        >
                          <i className="far fa-stop-circle" />
                        </button>
                      </div>
                    </div>
                    <span className="cluster-name">
                      This pipeline is running on: {workerClusterName}
                    </span>
                  </Operate>
                </Box>

                <Route
                  path={`${routeBaseUrl}/${jdbcSource}`}
                  render={() => (
                    <Connectors.JdbcSource
                      {...this.props}
                      {...connectorProps}
                      defs={this.getConnectorDefs({
                        connectors,
                        type: jdbcSource,
                      })}
                    />
                  )}
                />

                <Route
                  path={`${routeBaseUrl}/${ftpSource}`}
                  render={() => (
                    <Connectors.FtpSource
                      {...this.props}
                      {...connectorProps}
                      defs={this.getConnectorDefs({
                        connectors,
                        type: ftpSource,
                      })}
                    />
                  )}
                />

                <Route
                  path={`${routeBaseUrl}/${ftpSink}`}
                  render={() => (
                    <Connectors.FtpSink
                      {...this.props}
                      {...connectorProps}
                      defs={this.getConnectorDefs({
                        connectors,
                        type: ftpSink,
                      })}
                    />
                  )}
                />

                <Route
                  path={`${routeBaseUrl}/topic`}
                  render={() => (
                    <Connectors.Topic {...this.props} {...connectorProps} />
                  )}
                />

                <Route
                  path={`${routeBaseUrl}/${hdfsSink}`}
                  render={() => (
                    <Connectors.HdfsSink
                      {...this.props}
                      {...connectorProps}
                      defs={this.getConnectorDefs({
                        connectors,
                        type: hdfsSink,
                      })}
                    />
                  )}
                />

                <Route
                  path={`${routeBaseUrl}/streamApp`}
                  render={() => (
                    <Connectors.StreamApp {...this.props} {...connectorProps} />
                  )}
                />

                <Route
                  path={`${routeBaseUrl}/com.island.ohara.it.connector.(DumbSourceConnector|DumbSinkConnector)`}
                  render={() => (
                    <Connectors.CustomConnector
                      {...this.props}
                      {...connectorProps}
                      defs={this.getConnectorDefs({
                        connectors,
                        type: customSource,
                      })}
                    />
                  )}
                />
              </Sidebar>
            </Main>
          </Wrapper>
        </>
      </DocumentTitle>
    );
  }
}

export default PipelineNewPage;
