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

export const isSource = type => {
  return type === 'source';
};

export const isSink = type => {
  return type === 'sink';
};

export const isTopic = kind => kind === 'topic';

export const isStream = kind => kind === 'streamApp';

export const findByGraphId = (graph, id) => graph.find(x => x.id === id);

export const getConnectors = connectors => {
  const init = {
    sources: [],
    sinks: [],
    topics: [],
    streams: [],
  };

  const result = connectors.reduce((acc, connector) => {
    const { kind, id } = connector;

    if (isSource(kind)) {
      acc.sources.push(id);
    } else if (isSink(kind)) {
      acc.sinks.push(id);
    } else if (isStream(kind)) {
      acc.streams.push(id);
    } else if (isTopic(kind)) {
      // TODO: this should behave the same way as the rest of connectors
      acc.topics.push(connector);
    }

    return acc;
  }, init);

  return result;
};
