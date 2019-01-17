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
import styled from 'styled-components';
import DocumentTitle from 'react-document-title';
import toastr from 'toastr';
import PropTypes from 'prop-types';
import { Route } from 'react-router-dom';

import * as _ from 'utils/commonUtils';
import * as MESSAGES from 'constants/messages';
import * as PIPELINES from 'constants/pipelines';
import * as pipelinesApis from 'apis/pipelinesApis';
import * as topicApis from 'apis/topicApis';
import PipelineToolbar from './PipelineToolbar';
import PipelineGraph from './PipelineGraph';
import Editable from './Editable';
import { H2, H3 } from 'common/Headings';
import { Box } from 'common/Layout';
import { lightBlue, red, redHover, blue } from 'theme/variables';
import { PIPELINE_NEW, PIPELINE_EDIT } from 'constants/documentTitles';
import { JdbcSource, FtpSource, Topic, HdfsSink, FtpSink } from './connectors';
import {
  getConnectors,
  addPipelineStatus,
  updatePipelineParams,
  updateGraph,
  loadGraph,
} from 'utils/pipelineNewPageUtils';

const Wrapper = styled.div`
  padding-top: 75px;
  max-width: 1200px;
  width: calc(100% - 100px);
  margin: auto;
`;

Wrapper.displayName = 'Wrapper';

const Main = styled.div`
  display: flex;
`;

const Sidebar = styled.div`
  width: 35%;
`;

const Heading2 = styled(H2)`
  font-size: 16px;
  color: ${lightBlue};
`;

Heading2.displayName = 'H2';

const Heading3 = styled(H3)`
  font-size: 15px;
  font-weight: normal;
  margin: 0 0 15px;
  color: ${lightBlue};
`;

Heading3.displayName = 'H3';

const StartStopIcon = styled.button`
  color: ${({ isRunning }) => (isRunning ? red : lightBlue)};
  border: 0;
  font-size: 20px;
  cursor: pointer;
  background-color: transparent;

  &:hover {
    color: ${({ isRunning }) => (isRunning ? redHover : blue)};
  }
`;

class PipelineNewPage extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      isExact: PropTypes.bool,
      params: PropTypes.object,
      path: PropTypes.string,
      url: PropTypes.string,
    }).isRequired,
  };

  state = {
    topics: [],
    currentTopic: {},
    graph: [],
    isLoading: true,
    hasChanges: false,
    pipelines: {},
    pipelineTopics: [],
  };

  componentDidMount() {
    this.fetchData();
  }

  fetchData = async () => {
    const { match } = this.props;
    const pipelineId = _.get(match, 'params.pipelineId', null);

    const fetchTopicsPromise = this.fetchTopics();
    const fetchPipelinePromise = this.fetchPipeline(pipelineId);

    Promise.all([fetchTopicsPromise, fetchPipelinePromise]);
  };

  fetchTopics = async () => {
    const res = await topicApis.fetchTopics();
    this.setState(() => ({ isLoading: false }));

    const topics = _.get(res, 'data.result', null);
    if (topics) {
      this.setState({ topics, currentTopic: topics[0] });
    }
  };

  fetchPipeline = async pipelineId => {
    if (!pipelineId) return;

    const res = await pipelinesApis.fetchPipeline(pipelineId);
    const pipeline = _.get(res, 'data.result', null);

    if (pipeline) {
      const updatedPipeline = addPipelineStatus(pipeline);
      const { topics: pipelineTopics = [] } = getConnectors(
        updatedPipeline.objects,
      );

      this.setState({ pipelines: updatedPipeline, pipelineTopics }, () => {
        this.loadGraph(this.state.pipelines);
      });
    }
  };

  updateGraph = async (update, id) => {
    this.setState(({ graph }) => {
      return { graph: updateGraph(graph, update, id) };
    });
    await this.updatePipeline(update);
  };

  loadGraph = pipelines => {
    if (!pipelines) return;

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

  handleFocusOut = async isUpdate => {
    if (isUpdate) {
      return this.updatePipeline();
    }
  };

  updatePipeline = async update => {
    const { pipelines } = this.state;
    const { id, status } = pipelines;
    const params = updatePipelineParams(pipelines, update);

    const res = await pipelinesApis.updatePipeline({ id, params });
    const updatedPipelines = _.get(res, 'data.result', null);

    if (!_.isEmpty(updatedPipelines)) {
      const { topics: pipelineTopics } = getConnectors(
        updatedPipelines.objects,
      );

      // Keep the pipeline status since that's not stored on the configurator
      this.setState({
        pipelines: { ...updatedPipelines, status },
        pipelineTopics,
      });
    }
  };

  handleStartStopBtnClick = async () => {
    const pipelineId = _.get(this.props.match, 'params.pipelineId', null);
    await this.fetchPipeline(pipelineId);

    const { status, objects: connectors } = this.state.pipelines;

    if (!status) {
      toastr.error(
        'Failed to start the pipeline, please check your connectors settings',
      );

      return;
    }

    if (status === 'Stopped') {
      const res = await this.startConnectors(connectors);
      const isSuccess = res.filter(r => r.data.isSuccess);
      this.handleConnectorResponse(isSuccess, 'started');
    } else {
      const res = await this.stopConnectors(connectors);
      const isSuccess = res.filter(r => r.data.isSuccess);
      this.handleConnectorResponse(isSuccess, 'stopped');
    }
  };

  startConnectors = async connectors => {
    const { sources, sinks } = getConnectors(connectors);

    const sourcePromise = sources.map(source =>
      pipelinesApis.startSource(source),
    );
    const sinkPromise = sinks.map(sink => pipelinesApis.startSink(sink));

    return Promise.all([...sourcePromise, ...sinkPromise]).then(
      result => result,
    );
  };

  stopConnectors = connectors => {
    const { sources, sinks } = getConnectors(connectors);
    const sourcePromise = sources.map(source =>
      pipelinesApis.stopSource(source),
    );
    const sinkPromise = sinks.map(sink => pipelinesApis.stopSink(sink));
    return Promise.all([...sourcePromise, ...sinkPromise]).then(
      result => result,
    );
  };

  handleConnectorResponse = (isSuccess, action) => {
    if (isSuccess.length >= 2) {
      toastr.success(`Pipeline has been successfully ${action}!`);

      if (action === 'started') {
        this.setState(({ pipelines }) => {
          return {
            pipelines: {
              ...pipelines,
              status: 'Running',
            },
          };
        });
      } else if (action === 'stopped') {
        this.setState(({ pipelines }) => {
          return {
            pipelines: {
              ...pipelines,
              status: 'Stopped',
            },
          };
        });
      }

      const pipelineId = _.get(this.props.match, 'params.pipelineId', null);
      this.fetchPipeline(pipelineId);
    } else {
      toastr.error(MESSAGES.CANNOT_START_PIPELINE_ERROR);
    }
  };

  render() {
    const {
      isLoading,
      graph,
      topics,
      pipelineTopics,
      currentTopic,
      hasChanges,
      pipelines,
    } = this.state;

    if (_.isEmpty(pipelines)) return null;

    const pipelineId = _.get(this, 'props.match.params.pipelineId', null);
    const { name: pipelineTitle, status: pipelineStatus } = pipelines;

    const isPipelineRunning = pipelineStatus === 'Running' ? true : false;
    const startStopCls = isPipelineRunning
      ? 'fa-stop-circle'
      : 'fa-play-circle';

    const {
      jdbcSource,
      ftpSource,
      hdfsSink,
      ftpSink,
    } = PIPELINES.CONNECTOR_TYPES;

    return (
      <DocumentTitle title={pipelineId ? PIPELINE_EDIT : PIPELINE_NEW}>
        <React.Fragment>
          <Wrapper>
            <PipelineToolbar
              {...this.props}
              updateGraph={this.updateGraph}
              graph={graph}
              hasChanges={hasChanges}
              topics={topics}
              currentTopic={currentTopic}
              isLoading={isLoading}
              updateCurrentTopic={this.updateCurrentTopic}
            />

            <Main>
              <PipelineGraph
                {...this.props}
                graph={graph}
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
                  <Heading3>Operate</Heading3>
                  <StartStopIcon
                    isRunning={isPipelineRunning}
                    onClick={this.handleStartStopBtnClick}
                    data-testid="start-stop-icon"
                  >
                    <i className={`fa ${startStopCls}`} />
                  </StartStopIcon>
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
                      isLoading={isLoading}
                      isPipelineRunning={isPipelineRunning}
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
                      hasChanges={hasChanges}
                      isPipelineRunning={isPipelineRunning}
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
