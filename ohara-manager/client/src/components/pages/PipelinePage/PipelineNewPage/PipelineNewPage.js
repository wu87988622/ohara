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
import PropTypes from 'prop-types';
import { Prompt } from 'react-router-dom';
import { get, isEmpty } from 'lodash';

import * as MESSAGES from 'constants/messages';
import * as pipelineApi from 'api/pipelineApi';
import * as topicApi from 'api/topicApi';
import * as workerApi from 'api/workerApi';
import * as utils from './pipelineNewPageUtils';
import PipelineToolbar from '../PipelineToolbar';
import PipelineGraph from '../PipelineGraph';
import Operate from './Operate';
import SidebarRoutes from './SidebarRoutes';
import Metrics from './Metrics';
import { PIPELINE_NEW, PIPELINE_EDIT } from 'constants/documentTitles';
import { getConnectors } from '../pipelineUtils/commonUtils';
import { Wrapper, Main, Sidebar, Heading2 } from './styles';

class PipelineNewPage extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      params: PropTypes.shape({
        pipelineName: PropTypes.string.isRequired,
        connectorName: PropTypes.string,
      }).isRequired,
    }).isRequired,
  };

  state = {
    topics: [],
    currentTopic: null,
    graph: [],
    isLoading: true,
    isUpdating: false,
    hasChanges: false,
    hasRunningServices: false,
    pipeline: {},
    pipelineTopics: [],
    connectors: [],
    brokerClusterName: '',
    hasTimer: false,
  };

  async componentDidMount() {
    await this.fetchData();
    this.fetchPipelineWithInterval();
  }

  componentDidUpdate(prevProps, prevState) {
    const prevHasRunningServices = prevState.hasRunningServices;
    const nextHasRunningServices = this.state.hasRunningServices;

    if (prevHasRunningServices !== nextHasRunningServices) {
      if (this.state.hasRunningServices) {
        this.fetchPipelineWithInterval();
      } else {
        clearInterval(this.fetchPipelineTimer);
      }
    }
  }

  componentWillUnmount() {
    clearInterval(this.fetchPipelineTimer);
  }

  fetchData = async () => {
    await this.fetchPipeline();
    await this.fetchWorker();
    this.fetchTopics();
  };

  fetchPipelineWithInterval = () => {
    if (!this.state.hasRunningServices || this.state.hasTimer) return;

    this.fetchPipelineTimer = setInterval(async () => {
      await this.fetchPipeline();
    }, 5000);

    // Prevent duplicating timers
    this.setState({ hasTimer: true });
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
        const { topics: pipelineTopics = [] } = getConnectors(pipeline.objects);
        const hasRunningServices = pipeline.objects.some(object =>
          Boolean(object.state),
        );

        this.setState({ pipeline, pipelineTopics, hasRunningServices }, () => {
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
    const { connectorName } = this.props.match.params;
    this.setState({ graph: utils.loadGraph(pipeline, connectorName) });
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
    const { name } = pipeline;
    const params = utils.updatePipelineParams({ pipeline, ...update });

    this.setState({ isUpdating: true }, async () => {
      const res = await pipelineApi.updatePipeline({ name, params });

      this.setState({ isUpdating: false });
      const updatedPipelines = get(res, 'data.result', null);

      if (!isEmpty(updatedPipelines)) {
        const { topics: pipelineTopics } = getConnectors(
          updatedPipelines.objects,
        );

        this.setState({
          pipeline: updatedPipelines,
          pipelineTopics,
        });

        this.loadGraph(updatedPipelines);
      }
    });
  };

  updateHasRunningServices = update => {
    this.setState({ hasRunningServices: update });
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
      workerClusterName,
      objects: pipelineConnectors,
    } = pipeline;

    const connectorProps = {
      ...this.props,
      updateGraph: this.updateGraph,
      refreshGraph: this.refreshGraph,
      updateHasChanges: this.updateHasChanges,
      pipelineTopics: pipelineTopics,
      globalTopics: topics,
      pipeline,
      hasChanges,
      graph,
      connectors,
    };

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
              connectors={connectors}
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
                <Operate
                  pipelineName={pipelineName}
                  pipelineConnectors={pipelineConnectors}
                  workerClusterName={workerClusterName}
                  fetchPipeline={this.fetchPipeline}
                  updateHasRunningServices={this.updateHasRunningServices}
                />

                <Metrics
                  {...this.props}
                  graph={graph}
                  updateGraph={this.updateGraph}
                />

                <SidebarRoutes
                  {...this.props}
                  connectorProps={connectorProps}
                  connectors={connectors}
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
