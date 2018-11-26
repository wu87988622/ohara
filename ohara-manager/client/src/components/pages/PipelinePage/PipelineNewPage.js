import React from 'react';
import styled from 'styled-components';
import DocumentTitle from 'react-document-title';
import toastr from 'toastr';
import PropTypes from 'prop-types';
import { Route, Redirect } from 'react-router-dom';
import { v4 as uuid4 } from 'uuid';

import * as _ from 'utils/commonUtils';
import * as MESSAGES from 'constants/messages';
import PipelineSourcePage from './PipelineSourcePage';
import PipelineSourceFtpPage from './PipelineSourceFtpPage';
import PipelineTopicPage from './PipelineTopicPage';
import PipelineSinkPage from './PipelineSinkPage';
import PipelineSinkFtpPage from './PipelineSinkFtpPage';
import PipelineToolbar from './PipelineToolbar';
import PipelineGraph from './PipelineGraph';
import Editable from './Editable';
import { fetchTopic } from 'apis/topicApis';
import { H2, H3 } from 'common/Headings';
import { Box } from 'common/Layout';
import { isSource, isSink } from 'utils/pipelineUtils';
import { lightBlue, red, redHover, blue } from 'theme/variables';
import { PIPELINE } from 'constants/urls';
import { PIPELINE_NEW } from 'constants/documentTitles';
import { ICON_KEYS, ICON_MAPS } from 'constants/pipelines';
import {
  fetchPipeline,
  updatePipeline,
  startSource,
  startSink,
  stopSource,
  stopSink,
} from 'apis/pipelinesApis';

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

const LeftCol = styled.div`
  width: 65%;
  margin-right: 20px;
`;

const RightCol = styled.div`
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
    isRedirect: false,
    isLoading: true,
    hasChanges: false,
    pipelines: {},
  };

  componentDidMount() {
    const isValid = this.checkTopicId(this.props.match);

    if (isValid) {
      this.fetchData();
    }
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
    if (!_.isUuid(topicId)) return;

    const res = await fetchTopic(topicId);
    this.setState(() => ({ isLoading: false }));

    const result = _.get(res, 'data.result', null);

    if (!_.isNull(result)) {
      this.setState({ topicName: result.name });
    }
  };

  fetchPipeline = async pipelineId => {
    if (!_.isUuid(pipelineId)) return;

    const res = await fetchPipeline(pipelineId);
    const pipelines = _.get(res, 'data.result', null);

    if (pipelines) {
      const _pipelines = this.addPipelineStatus(pipelines);

      this.setState({ pipelines: _pipelines }, () => {
        this.loadGraph(this.state.pipelines);
      });
    }
  };

  addPipelineStatus = pipeline => {
    const status = pipeline.objects.filter(p => p.state === 'RUNNING');
    const _status = status.length >= 2 ? 'Running' : 'Stopped';

    return {
      ...pipeline,
      status: _status,
    };
  };

  checkTopicId = match => {
    const topicId = _.get(match, 'params.topicId', null);
    const isValid = !_.isNull(topicId) && _.isUuid(topicId);

    if (!isValid) {
      toastr.error(MESSAGES.TOPIC_ID_REQUIRED_ERROR);
      this.setState({ isRedirect: true });
      return false;
    }

    return true;
  };

  updateGraph = (update, id) => {
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
  };

  loadGraph = pipelines => {
    if (!pipelines) return;

    const { objects, rules } = pipelines;
    const { graph } = this.state;

    const _graph = objects.map(({ kind: type, uuid, name }, idx) => {
      return {
        name,
        type,
        uuid,
        icon: ICON_MAPS[type],
        id: graph[idx] ? graph[idx].id : uuid4(),
        isActive: graph[idx] ? graph[idx].isActive : false,
        to: '?',
      };
    });

    const froms = Object.keys(rules);

    const results = froms.map(from => {
      const source = _graph.filter(g => g.uuid === from);
      const target = _graph.filter(g => g.uuid === rules[from]);
      return {
        ...source[0],
        to: target[0] ? target[0].id : '',
      };
    });

    this.setState(
      () => {
        return { graph: _graph };
      },
      () => {
        results.forEach(result => this.updateGraph(result, result.id));
      },
    );
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

  updatePipeline = async () => {
    const { name, uuid, rules } = this.state.pipelines;
    const params = {
      name,
      rules,
    };

    const res = await updatePipeline({ uuid, params });
    const pipelines = _.get(res, 'data.result', []);

    if (!_.isEmpty(pipelines)) {
      this.setState({ pipelines });
    }
  };

  handleStartStopBtnClick = async pipeline => {
    const { objects: connectors, status } = pipeline;

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
    const { sources, sinks } = this.getConnectors(connectors);
    const sourcePromise = sources.map(source => startSource(source));
    const sinkPromise = sinks.map(sink => startSink(sink));

    return Promise.all([...sourcePromise, ...sinkPromise]).then(
      result => result,
    );
  };

  stopConnectors = connectors => {
    const { sources, sinks } = this.getConnectors(connectors);
    const sourcePromise = sources.map(source => stopSource(source));
    const sinkPromise = sinks.map(sink => stopSink(sink));
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
    } else {
      toastr.error(MESSAGES.CANNOT_START_PIPELINE_ERROR);
    }
  };

  getConnectors = connectors => {
    const sources = connectors
      .filter(({ kind }) => {
        return isSource(kind);
      })
      .map(({ uuid }) => uuid);

    const sinks = connectors
      .filter(({ kind }) => {
        return isSink(kind);
      })
      .map(({ uuid }) => uuid);

    return { sources, sinks };
  };

  render() {
    const {
      isLoading,
      graph,
      isRedirect,
      topicName,
      hasChanges,
      pipelines,
    } = this.state;

    if (isRedirect) {
      return <Redirect to={PIPELINE} />;
    }

    if (_.isEmpty(pipelines)) return null;

    const { name: pipelineTitle, status: pipelineStatus } = pipelines;

    const isPipelineRunning = pipelineStatus === 'Running' ? true : false;
    const startStopCls = isPipelineRunning
      ? 'fa-stop-circle'
      : 'fa-play-circle';

    const { jdbcSource, ftpSource, hdfsSink, ftpSink } = ICON_KEYS;

    return (
      <DocumentTitle title={PIPELINE_NEW}>
        <React.Fragment>
          <Wrapper>
            <PipelineToolbar
              {...this.props}
              updateGraph={this.updateGraph}
              graph={graph}
              hasChanges={hasChanges}
            />

            <Main>
              <LeftCol>
                <PipelineGraph
                  {...this.props}
                  graph={graph}
                  updateGraph={this.updateGraph}
                  resetGraph={this.resetGraph}
                />
              </LeftCol>

              <RightCol>
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
                    onClick={() => this.handleStartStopBtnClick(pipelines)}
                    data-testid="start-stop-icon"
                  >
                    <i className={`fa ${startStopCls}`} />
                  </StartStopIcon>
                </Box>

                <Route
                  path={`/pipelines/(new|edit)/${jdbcSource}`}
                  render={() => (
                    <PipelineSourcePage
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
                    <PipelineSourceFtpPage
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
                    <PipelineSinkFtpPage
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
                    <PipelineTopicPage
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
                    <PipelineSinkPage
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
              </RightCol>
            </Main>
          </Wrapper>
        </React.Fragment>
      </DocumentTitle>
    );
  }
}

export default PipelineNewPage;
