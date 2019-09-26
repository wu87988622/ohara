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

import React, { useState, useEffect, useCallback, useRef } from 'react';
import DocumentTitle from 'react-document-title';
import PropTypes from 'prop-types';
import { Prompt } from 'react-router-dom';
import { get, isEmpty, isEqual } from 'lodash';

import * as MESSAGES from 'constants/messages';
import * as pipelineApi from 'api/pipelineApi';
import * as topicApi from 'api/topicApi';
import * as workerApi from 'api/workerApi';
import * as utils from './pipelineEditPageUtils';
import PipelineToolbar from '../PipelineToolbar';
import PipelineGraph from '../PipelineGraph';
import Operate from './Operate';
import SidebarRoutes from './SidebarRoutes';
import Metrics from './Metrics';
import { usePrevious } from 'utils/hooks';
import { PIPELINE_EDIT } from 'constants/documentTitles';
import { Wrapper, Main, Sidebar, Heading2 } from './styles';
import NodeNames from './NodeNames';

const PipelineEditPage = props => {
  const [topics, setTopics] = useState([]);
  const [currentTopic, setCurrentTopic] = useState(null);
  const [graph, setGraph] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isUpdating, setIsUpdating] = useState(false);
  const [hasChanges, setHasChanges] = useState(false);
  const [hasRunningServices, setHasRunningServices] = useState(false);
  const [pipeline, setPipeline] = useState({});
  const [pipelineTopics, setPipelineTopics] = useState([]);
  const [connectors, setConnectors] = useState([]);
  const [freePorts, setFreePorts] = useState([]);
  const [nodeNames, setNodeNames] = useState([]);
  const [workerGroup, setWorkerGroup] = useState('default');
  const [brokerClusterName, setBrokerClusterName] = useState('');

  const { workspaceName, pipelineName, connectorName } = props.match.params;
  const group = `${workspaceName}${pipelineName}`;

  const fetchPipeline = useCallback(async () => {
    if (pipelineName) {
      const res = await pipelineApi.fetchPipeline(group, pipelineName);
      const pipeline = get(res, 'data.result', null);

      if (pipeline) {
        const pipelineTopics = pipeline.objects.filter(
          object => object.kind === 'topic',
        );

        const hasRunningServices = pipeline.objects
          .filter(object => object.kind !== 'topic') // topics are not counted as running objects
          .some(object => Boolean(object.state));

        setPipeline(pipeline);
        setPipelineTopics(pipelineTopics);
        setHasRunningServices(hasRunningServices);
      }
    }
  }, [group, pipelineName]);

  useEffect(() => {
    fetchPipeline();
  }, [fetchPipeline]);

  const loadGraph = useCallback(
    pipeline => {
      setGraph(utils.loadGraph(pipeline, connectorName));
    },
    [connectorName],
  );

  const previousPipeline = usePrevious(pipeline);

  useEffect(() => {
    if (isEmpty(pipeline) || isEqual(previousPipeline, pipeline)) return;

    // Pipeline data is ready, let's load the graph
    loadGraph(pipeline);

    const fetchWorker = async () => {
      const { tags } = pipeline;
      const { workerClusterName } = tags;
      const res = await workerApi.fetchWorker(workerClusterName);
      const worker = get(res, 'data.result', null);

      if (worker) {
        setFreePorts(
          get(worker, 'settings.freePorts', []).map(freePort =>
            freePort.toString(),
          ),
        );
        setNodeNames(get(worker, 'settings.nodeNames', []));
        setConnectors(worker.connectors);
        setBrokerClusterName(worker.settings.brokerClusterName);
        setWorkerGroup(worker.settings.group);
      }
    };

    fetchWorker();
  }, [loadGraph, pipeline, previousPipeline]);

  useEffect(() => {
    if (!brokerClusterName) return;

    const fetchTopics = async () => {
      const res = await topicApi.fetchTopics();
      setIsLoading(false);
      const topics = get(res, 'data.result', null);

      if (!isEmpty(topics)) {
        const topicsUnderBrokerCluster = topics.filter(
          topic => topic.brokerClusterName === brokerClusterName,
        );

        if (topicsUnderBrokerCluster) {
          setTopics(topicsUnderBrokerCluster);
          setCurrentTopic(topicsUnderBrokerCluster[0]);
        }
      }
    };

    fetchTopics();
  }, [brokerClusterName]);

  const prevHasRunningServices = usePrevious(hasRunningServices);

  // We're using `useRef` here so the value can be consistent acorss renders
  // The timer value then can be used in another `useEffect` which does the
  // cleanup seperately so the timer won't be cleand anytime a re-render is
  // run in the `fetchPipelineWithInterval` effect
  let fetchPipelineTimer = useRef(null);

  useEffect(() => {
    const fetchPipelineWithInterval = () => {
      const timer = setInterval(async () => {
        await fetchPipeline();
      }, 5000);

      fetchPipelineTimer.current = timer;
    };

    if (prevHasRunningServices !== hasRunningServices) {
      if (hasRunningServices) {
        fetchPipelineWithInterval();
      } else {
        clearInterval(fetchPipelineTimer.current);
      }
    }
  }, [fetchPipeline, hasRunningServices, prevHasRunningServices]);

  useEffect(() => {
    // Ensure the timer is removed when the page is unmounted
    return () => clearInterval(fetchPipelineTimer.current);
  }, []);

  const updateGraph = async params => {
    setGraph(prevGraph => {
      return utils.updateGraph({ graph: prevGraph, ...params });
    });

    await updatePipeline({ ...params });
  };

  const refreshGraph = () => {
    if (pipelineName) fetchPipeline(pipelineName);
  };

  const updatePipeline = async (update = {}) => {
    const { name, flows, group } = pipeline;
    const updatedFlows = utils.updateFlows({ pipeline, ...update });

    // Do not do the update if there's no need to do so,
    // we're only comparing flows here as the only field we're updating
    // is the flows field
    if (isEqual(updatedFlows, flows)) return;

    setIsUpdating(true);

    const response = await pipelineApi.updatePipeline({
      name,
      group,
      params: { flows: updatedFlows },
    });

    setIsUpdating(false);

    const updatedPipelines = get(response, 'data.result', null);

    if (!isEmpty(updatedPipelines)) {
      const pipelineTopics = updatedPipelines.objects.filter(
        object => object.kind === 'topic',
      );

      setPipeline(updatedPipelines);
      setPipelineTopics(pipelineTopics);
      loadGraph(updatedPipelines);
    }
  };

  const updateHasRunningServices = update => {
    setHasRunningServices(update);
  };

  if (isEmpty(pipeline) || isEmpty(connectors)) return null;

  const { name: pipelineTitle, tags } = pipeline;
  const { workerClusterName } = tags;

  const connectorProps = {
    ...props,
    updateGraph,
    refreshGraph,
    updateHasChanges: update => setHasChanges(update),
    pipelineTopics,
    globalTopics: topics,
    pipeline,
    hasChanges,
    graph,
    connectors,
    freePorts,
  };

  return (
    <DocumentTitle title={PIPELINE_EDIT}>
      <>
        <Prompt
          message={location =>
            location.pathname.startsWith('/pipelines/edit')
              ? true
              : MESSAGES.LEAVE_WITHOUT_SAVE
          }
          when={isUpdating}
        />
        <Wrapper>
          <PipelineToolbar
            {...props}
            updateGraph={updateGraph}
            graph={graph}
            hasChanges={hasChanges}
            topics={topics}
            currentTopic={currentTopic}
            isLoading={isLoading}
            resetCurrentTopic={() => setCurrentTopic(topics[0])}
            updateCurrentTopic={currentTopic => setCurrentTopic(currentTopic)}
            workerClusterName={workerClusterName}
            workerGroup={workerGroup}
            brokerClusterName={brokerClusterName}
            connectors={connectors}
          />

          <Main>
            <PipelineGraph
              {...props}
              graph={graph}
              pipeline={pipeline}
              updateGraph={updateGraph}
            />

            <Sidebar>
              <Heading2>{pipelineTitle}</Heading2>
              <Operate
                pipeline={pipeline}
                fetchPipeline={fetchPipeline}
                updateHasRunningServices={updateHasRunningServices}
              />

              <NodeNames {...props} graph={graph} nodeNames={nodeNames} />

              <Metrics {...props} graph={graph} updateGraph={updateGraph} />

              <SidebarRoutes
                {...props}
                connectorProps={connectorProps}
                connectors={connectors}
              />
            </Sidebar>
          </Main>
        </Wrapper>
      </>
    </DocumentTitle>
  );
};

PipelineEditPage.propTypes = {
  match: PropTypes.shape({
    params: PropTypes.shape({
      pipelineName: PropTypes.string.isRequired,
      workspaceName: PropTypes.string.isRequired,
      connectorName: PropTypes.string,
    }).isRequired,
  }).isRequired,
};

export default PipelineEditPage;
