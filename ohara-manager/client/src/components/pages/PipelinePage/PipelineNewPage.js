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
import PipelineJdbcSource from './PipelineJdbcSource';
import PipelineFtpSource from './PipelineFtpSource';
import PipelineTopic from './PipelineTopic';
import PipelineHdfsSink from './PipelineHdfsSink';
import PipelineFtpSink from './PipelineFtpSink';
import PipelineToolbar from './PipelineToolbar';
import PipelineGraph from './PipelineGraph';
import Editable from './Editable';
import { getConnectors, addPipelineStatus } from 'utils/pipelineNewPageUtils';
import { fetchTopic } from 'apis/topicApis';
import { H2, H3 } from 'common/Headings';
import { Box } from 'common/Layout';
import { isSink } from 'utils/pipelineUtils';
import { lightBlue, red, redHover, blue } from 'theme/variables';
import { PIPELINE_NEW, PIPELINE_EDIT } from 'constants/documentTitles';

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
    topicName: '',
    graph: [],
    isLoading: true,
    hasChanges: false,
    pipelines: {},
  };

  componentDidMount() {
    this.fetchData();
  }

  fetchData = async () => {
    const { match } = this.props;
    const topicId = _.get(match, 'params.topicId', null);
    const pipelineId = _.get(match, 'params.pipelineId', null);

    const fetchTopicsPromise = this.fetchTopics(topicId);
    const fetchPipelinePromise = this.fetchPipeline(pipelineId);

    Promise.all([fetchTopicsPromise, fetchPipelinePromise]);
  };

  fetchTopics = async topicId => {
    if (!topicId) return;

    const res = await fetchTopic(topicId);
    this.setState(() => ({ isLoading: false }));

    const result = _.get(res, 'data.result', null);

    if (!_.isNull(result)) {
      this.setState({ topicName: result.name });
    }
  };

  fetchPipeline = async pipelineId => {
    if (!pipelineId) return;

    const res = await pipelinesApis.fetchPipeline(pipelineId);
    const pipelines = _.get(res, 'data.result', null);

    if (pipelines) {
      const _pipelines = addPipelineStatus(pipelines);

      this.setState({ pipelines: _pipelines }, () => {
        this.loadGraph(this.state.pipelines);
      });
    }
  };

  updateGraph = async (update, id) => {
    this.setState(({ graph }) => {
      const idx = graph.findIndex(g => g.id === id);
      let _graph = [];

      if (idx === -1) {
        _graph = [...graph, update];
      } else {
        _graph = [
          ...graph.slice(0, idx),
          { ...graph[idx], ...update },
          ...graph.slice(idx + 1),
        ];
      }

      return {
        graph: _graph,
      };
    });

    await this.updatePipeline(update);
  };

  loadGraph = pipelines => {
    if (!pipelines) return;

    const { objects } = pipelines;
    const { graph } = this.state;

    let updatedGraph;

    if (_.isEmpty(objects) && !_.isEmpty(graph[0])) {
      const { name, type, id } = graph[0];
      updatedGraph = [
        {
          state: '',
          name: name,
          type: type,
          icon: PIPELINES.ICON_MAPS[type],
          id,
          isActive: false,
          to: '?',
        },
      ];
    } else {
      updatedGraph = objects.map(
        ({ kind: type, id, name, state = '' }, idx) => {
          return {
            state,
            name,
            type,
            id,
            icon: PIPELINES.ICON_MAPS[type],
            isActive: graph[idx] ? graph[idx].isActive : false,
            to: '?',
          };
        },
      );
    }

    this.setState(() => {
      return { graph: updatedGraph };
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
      const _pipelines = { ...pipelines, name: title };
      return { pipelines: _pipelines };
    });
  };

  updateHasChanges = update => {
    this.setState({ hasChanges: update });
  };

  handleFocusOut = async isUpdate => {
    if (isUpdate) {
      return this.updatePipeline();
    }
  };

  updatePipeline = async update => {
    const { name, id, rules, status } = this.state.pipelines;

    let params;
    if (update && update.id) {
      const { id, type } = update;
      const updateRule = isSink(type) ? { '?': id } : { [id]: '?' };

      params = {
        name,
        rules: { ...rules, ...updateRule },
      };
    } else {
      params = { name, rules };
    }

    const res = await pipelinesApis.updatePipeline({ id, params });
    const pipelines = _.get(res, 'data.result', null);

    if (!_.isEmpty(pipelines)) {
      // Keep the pipeline status since that's not stored on the configurator
      this.setState({ pipelines: { ...pipelines, status } });
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
    const { isLoading, graph, topicName, hasChanges, pipelines } = this.state;

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
                    <PipelineJdbcSource
                      {...this.props}
                      graph={graph}
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
                    <PipelineFtpSource
                      {...this.props}
                      graph={graph}
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
                    <PipelineFtpSink
                      {...this.props}
                      graph={graph}
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
                    <PipelineTopic
                      {...this.props}
                      graph={graph}
                      loadGraph={this.loadGraph}
                      updateGraph={this.updateGraph}
                      isLoading={isLoading}
                      name={topicName}
                      isPipelineRunning={isPipelineRunning}
                    />
                  )}
                />

                <Route
                  path={`/pipelines/(new|edit)/${hdfsSink}`}
                  render={() => (
                    <PipelineHdfsSink
                      {...this.props}
                      graph={graph}
                      hasChanges={hasChanges}
                      loadGraph={this.loadGraph}
                      updateGraph={this.updateGraph}
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
