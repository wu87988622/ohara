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

import { isNull, isEmpty, isString } from 'lodash';

import { isTopic, isStream } from '../pipelineUtils/commonUtils';

export const removePrevConnector = (flows, connectorName) => {
  const updateFlows = flows.map(flow => {
    const hasConnection = flow.to.find(t => t.name === connectorName);

    if (hasConnection) {
      const updatedTo = flow.to.filter(t => t.name !== connectorName);
      return { ...flow, to: updatedTo };
    }
    return flow;
  });

  return updateFlows;
};

export const updateFlows = ({
  pipeline,
  update = null,
  sinkName = null,
  streamAppName = null,
  dispatcher,
}) => {
  let { flows } = pipeline;

  // Remove previous connector from the graph as we're only allowing single
  // connection between a connector to a topic for now
  if (!isNull(sinkName) || !isNull(streamAppName)) {
    const connectorName = sinkName || streamAppName;
    flows = removePrevConnector(flows, connectorName);
  }

  const { name: connectorName, to } = update;
  let updatedFlows = null;

  if (dispatcher.name === 'TOOLBAR') {
    // Toolbar update only needs to `add` new connector to the graph
    updatedFlows = [
      ...flows,
      { from: { group: 'default', name: connectorName }, to: [] },
    ];
  } else if (dispatcher.name === 'CONNECTOR') {
    updatedFlows = flows.map(flow => {
      if (flow.from.name === connectorName) {
        const newTo = to.map(to => {
          // If the to is a string, let's wrap it with an object
          if (isString(to)) return { group: 'default', name: to };
          return to;
        });

        // 1. reset the `to` to an empty array which means there's no
        // topic connect to this connector
        // 2. use the new to
        const updatedTo = isEmpty(to) ? [] : [...newTo];

        const update = {
          ...flow,
          to: updatedTo,
        };

        return update;
      }

      return flow;
    });
  }

  return updatedFlows;
};

export const updateSingleGraph = (graph, name, transformer) => {
  return graph.map(g => {
    if (g.name === name) {
      return { ...transformer(g) };
    }

    return g;
  });
};

export const cleanPrevFromTopics = (graph, connectorName) => {
  // See if the connectorName is connected with a topic in the graph
  const prevTopic = graph.find(g => g.to.includes(connectorName));

  if (prevTopic) {
    // Remove previous "form topic"
    const prevTopicTo = prevTopic.to.filter(t => t !== connectorName);
    const transformer = g => ({ ...g, to: prevTopicTo });
    const updatedGraph = updateSingleGraph(graph, prevTopic.name, transformer);

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
  streamAppName = null,
  sinkName = null,
}) => {
  let updatedGraph;

  // From topic update -- sink connectors or the fromTopic in stream apps
  if (isFromTopic) {
    const connectorName = sinkName || streamAppName;

    // Update the sink connector name
    const nameTransformer = g => ({ ...g, name: updatedName });
    updatedGraph =
      updateSingleGraph(graph, connectorName, nameTransformer) || graph;

    // clean up previous connections
    updatedGraph = cleanPrevFromTopics(updatedGraph, connectorName);

    // Update current topic
    const toTransformer = g => ({ ...g, to: update.to });
    updatedGraph = updateSingleGraph(updatedGraph, update.name, toTransformer);
  } else {
    const target = graph.find(g => g.name === update.name);

    // Adds the connector to graph
    if (isEmpty(target)) {
      updatedGraph = [...graph, update];
    } else {
      // Updates the target connector
      const transformer = g => ({ ...g, ...update });
      updatedGraph = updateSingleGraph(graph, target.name, transformer);
    }
  }

  return updatedGraph;
};

export const loadGraph = (pipeline, currentConnectorName) => {
  const { objects, flows } = pipeline;

  // temp fix, if the flows and objects are not the same
  if (isEmpty(objects) && !isEmpty(flows)) return [];

  const graph = flows.map((flow, index) => {
    const target = objects.find(object => object.name === flow.from.name);
    const { kind } = target;
    const isActive = flow.from.name === currentConnectorName;

    const props = {
      ...target,
      isActive,
      to: flows[index].to,
    };

    if (isTopic(kind) || isStream(kind)) {
      return {
        ...props,
        className: kind,
      };
    }

    return { ...props };
  });

  return graph;
};
