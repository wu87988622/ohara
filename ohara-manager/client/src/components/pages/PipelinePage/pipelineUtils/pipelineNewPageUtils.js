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

import { isNull, isEmpty } from 'lodash';

import { isTopic, isStream } from './commonUtils';

export const addPipelineStatus = pipeline => {
  const status = pipeline.objects.filter(p => p.state === 'RUNNING');
  const updatedStatus = status.length >= 2 ? 'Running' : 'Stopped';

  return {
    ...pipeline,
    status: updatedStatus,
  };
};

export const removePrevConnector = (rules, connectorId) => {
  const updatedRule = Object.keys(rules)
    .map(key => {
      // Remove the previous sinkId, if it exists
      if (rules[key].includes(connectorId)) {
        const updatedTo = rules[key].filter(rule => rule !== connectorId);
        const result = { [key]: updatedTo };
        return result;
      }

      return {
        [key]: rules[key],
      };
    })
    .reduce((acc, rule) => {
      // Change the result data structure
      acc = { ...acc, ...rule };
      return acc;
    }, {});

  return updatedRule;
};

export const updatePipelineParams = ({
  pipelines,
  update = null,
  sinkId = null,
  streamAppId = null,
}) => {
  let params = null;
  let { rules } = pipelines;

  // Remove previous connector from the graph as we're only allowing single
  // connection in v0.2
  if (!isNull(sinkId) || !isNull(streamAppId)) {
    const connectorId = sinkId || streamAppId;
    rules = removePrevConnector(rules, connectorId);
  }

  // Extract necessary props for later update
  // This is due to we currently have both new and old API in the pipelines
  // When they're putting together, it would reset pipeline graph on each update...
  const { id, name, status, workerClusterName } = pipelines;
  const pipelineProps = { id, name, status, workerClusterName };

  // If update is not specify, just pass down the entire pipeline
  if (isNull(update)) {
    params = pipelines;
  } else {
    const { id, to } = update;
    const updatedRule = { [id]: to };

    params = {
      ...pipelineProps,
      rules: { ...rules, ...updatedRule },
    };
  }

  return params;
};

export const updateSingleGraph = (graph, id, transformer) => {
  return graph.map(g => {
    if (g.id === id) {
      return { ...transformer(g) };
    }

    return g;
  });
};

export const cleanPrevFromTopics = (graph, connectorId) => {
  // See if the connectorId is connected with a topic in the graph
  const prevTopic = graph.find(g => g.to.includes(connectorId));

  if (prevTopic) {
    // Remove previous "form topic"
    const prevTopicTo = prevTopic.to.filter(t => t !== connectorId);
    const transformer = g => ({ ...g, to: prevTopicTo });
    const updatedGraph = updateSingleGraph(graph, prevTopic.id, transformer);

    return updatedGraph;
  }

  // if there's no update, return the graph
  return graph;
};

export const updateGraph = ({
  graph,
  update,
  updatedName,
  isFromTopic,
  streamAppId = null,
  sinkId = null,
}) => {
  let updatedGraph;

  // From topic update -- sink connectors or the fromTopic in stream apps
  if (isFromTopic) {
    const connectorId = sinkId || streamAppId;

    // Update the sink connector name
    const nameTransformer = g => ({ ...g, name: updatedName });
    updatedGraph =
      updateSingleGraph(graph, connectorId, nameTransformer) || graph;

    // clean up previous connections
    updatedGraph = cleanPrevFromTopics(updatedGraph, connectorId);

    // Update current topic
    const toTransformer = g => ({ ...g, to: update.to });
    updatedGraph = updateSingleGraph(updatedGraph, update.id, toTransformer);
  } else {
    const target = graph.find(g => g.id === update.id);

    // Adds the connector to graph
    if (isEmpty(target)) {
      updatedGraph = [...graph, update];
    } else {
      // Updates the target connector
      const transformer = g => ({ ...g, ...update });
      updatedGraph = updateSingleGraph(graph, target.id, transformer);
    }
  }

  return updatedGraph;
};

export const loadGraph = pipelines => {
  const { objects, rules } = pipelines;

  const updatedGraph = Object.keys(rules).map(x => {
    const target = objects.find(object => object.id === x);

    // Add a to prop in local state so we can create graph with this prop
    const { kind } = target;

    const props = {
      ...target,
      to: rules[x],
    };

    if (isTopic(kind) || isStream(kind)) {
      return {
        ...props,
        className: kind,
      };
    }

    return { ...props };
  });

  return updatedGraph;
};
