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
import { H2 } from 'common/Headings';
import { lightBlue } from 'theme/variables';
import { PIPELINE } from 'constants/urls';
import { PIPELINE_NEW } from 'constants/documentTitles';
import { ICON_KEYS, ICON_MAPS } from 'constants/pipelines';
import { fetchPipeline, updatePipeline } from 'apis/pipelinesApis';

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
      this.setState({ pipelines }, () => {
        this.loadGraph(pipelines);
      });
    }
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

  render() {
    const {
      isLoading,
      graph,
      isRedirect,
      topicName,
      hasChanges,
      pipelines,
    } = this.state;

    const pipelineTitle = _.get(pipelines, 'name', '');

    const { jdbcSource, ftpSource, hdfsSink, ftpSink } = ICON_KEYS;

    if (isRedirect) {
      return <Redirect to={PIPELINE} />;
    }

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
                  updateG={this.updateG}
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
                <Route
                  path={`/pipelines/(new|edit)/${jdbcSource}`}
                  render={() => (
                    <PipelineSourcePage
                      {...this.props}
                      graph={graph}
                      loadGraph={this.loadGraph}
                      updateGraph={this.updateGraph}
                      hasChanges={hasChanges}
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
