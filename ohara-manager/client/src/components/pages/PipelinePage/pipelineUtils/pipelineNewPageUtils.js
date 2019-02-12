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

export const removePrevSinkConnection = (rules, sinkId) => {
  const updatedRule = Object.keys(rules)
    .map(key => {
      // Remove the previous sinkId, if it exists
      if (rules[key].includes(sinkId)) {
        const updatedTo = rules[key].filter(rule => rule !== sinkId);
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
}) => {
  let params = null;
  let { rules } = pipelines;

  // Remove previous sink connection from graph
  if (!isNull(sinkId)) {
    rules = removePrevSinkConnection(rules, sinkId);
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
