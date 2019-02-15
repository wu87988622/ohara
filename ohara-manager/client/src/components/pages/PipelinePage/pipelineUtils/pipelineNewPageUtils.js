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

import { isSource, isSink, isTopic, isStream } from './commonUtils';
import { isEmptyStr } from 'utils/commonUtils';

export const getConnectors = connectors => {
  const sources = connectors
    .filter(({ kind }) => {
      return isSource(kind);
    })
    .map(({ id }) => id);

  const sinks = connectors
    .filter(({ kind }) => {
      return isSink(kind);
    })
    .map(({ id }) => id);

  const topics = connectors.filter(({ kind }) => {
    return isTopic(kind);
  });

  return { sources, sinks, topics };
};

export const addPipelineStatus = pipeline => {
  const status = pipeline.objects.filter(p => p.state === 'RUNNING');
  const _status = status.length >= 2 ? 'Running' : 'Stopped';

  return {
    ...pipeline,
    status: _status,
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

  // If update is not specify, just pass down the entire pipeline
  if (isNull(update)) {
    params = pipelines;
  } else {
    const { id, to } = update;
    const updatedRule = { [id]: to };

    params = {
      ...pipelines,
      rules: { ...rules, ...updatedRule },
    };
  }

  return params;
};

const updateConnectorName = (graph, updatedName, connectorId) => {
  return graph.map(g => {
    if (g.id === connectorId) {
      return { ...g, name: updatedName };
    }

    return g;
  });
};

const updateSingleGraph = (graph, targetIdx, key, value) => {
  return graph.map((g, idx) => {
    if (idx === targetIdx) {
      return { ...g, [key]: value };
    }

    return g;
  });
};

const cleanPrevConnection = (graph, connectorId, updatedName) => {
  // Update the sink connector name
  let updatedGraph =
    updateConnectorName(graph, updatedName, connectorId) || graph;

  // Remove sink from other topics since our UI doesn't support this logic yet
  if (connectorId) {
    const prevTopicIdx = updatedGraph.findIndex(g =>
      g.to.includes(connectorId),
    );

    if (prevTopicIdx !== -1) {
      const prevTopicTo = updatedGraph[prevTopicIdx].to.filter(
        t => t !== connectorId,
      );

      updatedGraph = updateSingleGraph(
        updatedGraph,
        prevTopicIdx,
        'to',
        prevTopicTo,
      );
    }
  }

  return updatedGraph;
};

export const updateGraph = ({
  graph,
  update,
  updatedName,
  isSinkUpdate = false,
  isStreamAppFromUpdate = false,
  streamAppId = null,
  sinkId = null,
}) => {
  let updatedGraph;

  if (isSinkUpdate || isStreamAppFromUpdate) {
    const connectorId = sinkId || streamAppId;

    // clean up previous connections
    updatedGraph = cleanPrevConnection(graph, connectorId, updatedName);

    // Update current topic
    const topicIdx = updatedGraph.findIndex(g => g.id === update.id);
    updatedGraph = updateSingleGraph(updatedGraph, topicIdx, 'to', update.to);
  } else {
    const target = graph.find(g => g.id === update.id);

    if (isEmpty(target)) {
      updatedGraph = [...graph, update];
    } else {
      updatedGraph = graph.map(g => {
        if (g.id === target.id) {
          return { ...g, ...update };
        }

        return g;
      });
    }
  }

  return updatedGraph;
};

export const loadGraph = pipelines => {
  const { objects, rules } = pipelines;
  const updatedGraph = Object.keys(rules).map(x => {
    const target = objects.find(object => object.id === x);

    // Stream doesn't have a default name, we should provide one
    if (isStream(target.kind) && isEmptyStr(target.name)) {
      return {
        ...target,
        to: rules[x],
        name: 'Untitled stream app',
      };
    }

    return {
      ...target,
      to: rules[x],
    };
  });

  return updatedGraph;
};
