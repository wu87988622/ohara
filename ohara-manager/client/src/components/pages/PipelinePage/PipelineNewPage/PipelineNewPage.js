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
import PipelineToolbar from '../PipelineToolbar';
import PipelineGraph from '../PipelineGraph';
import Editable from '../Editable';
import { Box } from 'common/Layout';
import { getConnectors } from '../pipelineUtils/commonUtils';
import { PIPELINE_NEW, PIPELINE_EDIT } from 'constants/documentTitles';
import {
  JdbcSource,
  FtpSource,
  Topic,
  HdfsSink,
  FtpSink,
  StreamApp,
} from '../Connectors';
import {
  addPipelineStatus,
  updatePipelineParams,
  updateGraph,
  loadGraph,
} from '../pipelineUtils/pipelineNewPageUtils';

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
    pipelines: {},
    pipelineTopics: [],
  };

  componentDidMount() {
    this.fetchData();
  }

  fetchData = async () => {
    this.fetchTopics();
    this.fetchPipeline();
  };

  fetchTopics = async () => {
    const res = await topicApi.fetchTopics();
    this.setState(() => ({ isLoading: false }));

    const topics = get(res, 'data.result', null);
    if (topics) {
      this.setState({ topics, currentTopic: topics[0] });
    }
  };

  fetchPipeline = async () => {
    const { match } = this.props;
    const pipelineId = get(match, 'params.pipelineId', null);

    if (pipelineId) {
      const res = await pipelineApi.fetchPipeline(pipelineId);
      const pipeline = get(res, 'data.result', null);

      if (pipeline) {
        const updatedPipeline = addPipelineStatus(pipeline);
        const { topics: pipelineTopics = [] } = getConnectors(
          updatedPipeline.objects,
        );

        this.setState({ pipelines: updatedPipeline, pipelineTopics }, () => {
          this.loadGraph(this.state.pipelines);
        });
      }
    }
  };

  updateGraph = async params => {
    this.setState(({ graph }) => {
      return {
        graph: updateGraph({
          graph,
          ...params,
        }),
      };
    });

    await this.updatePipeline({ ...params });
  };

  loadGraph = pipelines => {
    this.setState(() => {
      return { graph: loadGraph(pipelines) };
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
    const { pipelineId } = this.props.match.params;
    if (pipelineId) {
      this.fetchPipeline(pipelineId);
    }
  };

  handlePipelineTitleChange = ({ target: { value: title } }) => {
    this.setState(({ pipelines }) => {
      const updatedPipeline = { ...pipelines, name: title };
      return { pipelines: updatedPipeline };
    });
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

  handleFocusOut = async isUpdate => {
    if (isUpdate) {
      return this.updatePipeline();
    }
  };

  updatePipeline = async (update = {}) => {
    const { pipelines } = this.state;
    const { id, status } = pipelines;
    const params = updatePipelineParams({ pipelines, ...update });

    this.setState({ isUpdating: true }, async () => {
      const res = await pipelineApi.updatePipeline({ id, params });
      this.setState({ isUpdating: false });
      const updatedPipelines = get(res, 'data.result', null);

      if (!isEmpty(updatedPipelines)) {
        const { topics: pipelineTopics } = getConnectors(
          updatedPipelines.objects,
        );

        // Keep the pipeline status since that's not stored on the configurator
        this.setState({
          pipelines: { ...updatedPipelines, status },
          pipelineTopics,
        });
      }
    });
  };

  checkPipelineStatus = async () => {
    const pipelineId = get(this.props.match, 'params.pipelineId', null);
    await this.fetchPipeline(pipelineId);

    const { status, objects: connectors } = this.state.pipelines;

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

    const sourcePromises = sources.map(source =>
      connectorApi.startConnector(source),
    );
    const sinkPromises = sinks.map(sink => connectorApi.startConnector(sink));
    const streamsPromises = streams.map(stream => streamApi.start(stream));

    return Promise.all([
      ...sourcePromises,
      ...sinkPromises,
      ...streamsPromises,
    ]).then(result => result);
  };

  stopConnectors = connectors => {
    const { sources, sinks, streams } = getConnectors(connectors);
    this.updateRunningConnectors(sources, sinks, streams);

    const sourcePromises = sources.map(source =>
      connectorApi.stopConnector(source),
    );
    const sinkPromises = sinks.map(sink => connectorApi.stopConnector(sink));
    const streamsPromises = streams.map(stream => streamApi.stop(stream));

    return Promise.all([
      ...sourcePromises,
      ...sinkPromises,
      ...streamsPromises,
    ]).then(result => result);
  };

  handleConnectorResponse = (isSuccess, action) => {
    if (isSuccess.length === this.state.runningConnectors) {
      toastr.success(`Pipeline has been successfully ${action}!`);
      let status = action === 'started' ? 'Running' : 'Stopped';

      this.setState(({ pipelines }) => {
        return {
          runningConnectors: 0,
          pipelines: {
            ...pipelines,
            status,
          },
        };
      });

      const pipelineId = get(this.props.match, 'params.pipelineId', null);
      this.fetchPipeline(pipelineId);
    } else {
      toastr.error(MESSAGES.CANNOT_START_PIPELINE_ERROR);
    }
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
      pipelines,
    } = this.state;

    if (isEmpty(pipelines)) return null;

    const pipelineId = get(this, 'props.match.params.pipelineId', null);
    const {
      name: pipelineTitle,
      status: pipelineStatus,
      workerClusterName,
    } = pipelines;

    const isPipelineRunning = pipelineStatus === 'Running' ? true : false;

    const {
      jdbcSource,
      ftpSource,
      hdfsSink,
      ftpSink,
    } = PIPELINES.CONNECTOR_TYPES;

    return (
      <DocumentTitle title={pipelineId ? PIPELINE_EDIT : PIPELINE_NEW}>
        <React.Fragment>
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
              currWorkerClusterName={workerClusterName}
            />

            <Main>
              <PipelineGraph
                {...this.props}
                graph={graph}
                pipeline={pipelines}
                updateGraph={this.updateGraph}
                resetGraph={this.resetGraph}
              />

              <Sidebar>
                <Heading2>
                  <Editable
                    title={pipelineTitle}
                    handleFocusOut={this.handleFocusOut}
                    handleChange={this.handlePipelineTitleChange}
                  />
                </Heading2>

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
                  path={`/pipelines/(new|edit)/${jdbcSource}`}
                  render={() => (
                    <JdbcSource
                      {...this.props}
                      graph={graph}
                      topics={pipelineTopics}
                      loadGraph={this.loadGraph}
                      updateGraph={this.updateGraph}
                      refreshGraph={this.refreshGraph}
                      hasChanges={hasChanges}
                      isPipelineRunning={isPipelineRunning}
                      updateHasChanges={this.updateHasChanges}
                    />
                  )}
                />

                <Route
                  path={`/pipelines/(new|edit)/${ftpSource}`}
                  render={() => (
                    <FtpSource
                      {...this.props}
                      graph={graph}
                      topics={pipelineTopics}
                      loadGraph={this.loadGraph}
                      updateGraph={this.updateGraph}
                      refreshGraph={this.refreshGraph}
                      hasChanges={hasChanges}
                      isPipelineRunning={isPipelineRunning}
                      updateHasChanges={this.updateHasChanges}
                    />
                  )}
                />

                <Route
                  path={`/pipelines/(new|edit)/${ftpSink}`}
                  render={() => (
                    <FtpSink
                      {...this.props}
                      graph={graph}
                      topics={pipelineTopics}
                      loadGraph={this.loadGraph}
                      updateGraph={this.updateGraph}
                      refreshGraph={this.refreshGraph}
                      hasChanges={hasChanges}
                      isPipelineRunning={isPipelineRunning}
                      updateHasChanges={this.updateHasChanges}
                    />
                  )}
                />

                <Route
                  path="/pipelines/(new|edit)/topic"
                  render={() => (
                    <Topic
                      {...this.props}
                      pipeline={pipelines}
                      graph={graph}
                      refreshGraph={this.refreshGraph}
                    />
                  )}
                />

                <Route
                  path={`/pipelines/(new|edit)/${hdfsSink}`}
                  render={() => (
                    <HdfsSink
                      {...this.props}
                      graph={graph}
                      topics={pipelineTopics}
                      loadGraph={this.loadGraph}
                      updateGraph={this.updateGraph}
                      refreshGraph={this.refreshGraph}
                      hasChanges={hasChanges}
                      isPipelineRunning={isPipelineRunning}
                      updateHasChanges={this.updateHasChanges}
                    />
                  )}
                />

                <Route
                  path={`/pipelines/(new|edit)/streamApp`}
                  render={() => (
                    <StreamApp
                      {...this.props}
                      graph={graph}
                      topics={pipelineTopics}
                      updateGraph={this.updateGraph}
                      refreshGraph={this.refreshGraph}
                      updateHasChanges={this.updateHasChanges}
                    />
                  )}
                />
              </Sidebar>
            </Main>
          </Wrapper>
        </React.Fragment>
      </DocumentTitle>
    );
  }
}

export default PipelineNewPage;
