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

import { isNull } from 'lodash';

import { isSource, isSink, isTopic } from './pipelineUtils';

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

export const updatePipelineParams = (pipelines, update = null) => {
  let params = null;

  // If update is not specify, just pass down the entire pipeline
  if (isNull(update)) {
    params = pipelines;
  } else {
    const { rules } = pipelines;
    const { id, to } = update;
    const updateRule = { [id]: to };

    params = {
      ...pipelines,
      rules: { ...rules, ...updateRule },
    };
  }

  return params;
};

const updateSinkName = (graph, updatedName, sinkId) => {
  if (isNull(sinkId)) return;

  const sinkIdx = graph.findIndex(g => g.id === sinkId);
  const result = [
    ...graph.slice(0, sinkIdx),
    { ...graph[sinkIdx], name: updatedName },
    ...graph.slice(sinkIdx + 1),
  ];

  return result;
};

const updateSingleGraph = (graph, idx, key, value) => {
  const result = [
    ...graph.slice(0, idx),
    {
      ...graph[idx],
      [key]: value,
    },
    ...graph.slice(idx + 1),
  ];

  return result;
};

export const updateGraph = ({
  graph,
  update,
  updatedName,
  isSinkUpdate = false,
  sinkId = null,
}) => {
  let updatedGraph;
  let idx = null;

  if (isSinkUpdate) {
    // Update the sink connector name
    updatedGraph = updateSinkName(graph, updatedName, sinkId) || graph;

    // Remove sink from other topics since our UI doesn't support this logic yet
    if (sinkId) {
      const prevTopicIdx = updatedGraph.findIndex(g => g.to.includes(sinkId));
      const prevTopicTo = updatedGraph[prevTopicIdx].to.filter(
        t => t !== sinkId,
      );

      updatedGraph = updateSingleGraph(
        updatedGraph,
        prevTopicIdx,
        'to',
        prevTopicTo,
      );
    }

    // Update current topic
    const topicIdx = updatedGraph.findIndex(g => g.id === update.id);
    updatedGraph = updateSingleGraph(updatedGraph, topicIdx, 'to', update.to);
  } else {
    idx = graph.findIndex(g => g.id === update.id);

    if (idx === -1) {
      updatedGraph = [...graph, update];
    } else {
      updatedGraph = [
        ...graph.slice(0, idx),
        { ...graph[idx], ...update },
        ...graph.slice(idx + 1),
      ];
    }
  }

  return updatedGraph;
};

export const loadGraph = pipelines => {
  const { objects, rules } = pipelines;
  const updatedGraph = Object.keys(rules).map(x => {
    const target = objects.find(object => object.id === x);

    if (rules[x] !== '?') {
      return {
        ...target,
        to: rules[x],
      };
    }

    return {
      ...target,
      to: '?',
    };
  });

  return updatedGraph;
};
