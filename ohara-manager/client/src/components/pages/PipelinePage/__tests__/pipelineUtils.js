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

import { findByGraphName, getConnectors } from '../pipelineUtils';

describe('findByGraphName()', () => {
  it('finds the right connector by name', () => {
    const graph = [{ name: 'a' }, { name: 'b' }, { name: 'c' }];
    expect(findByGraphName(graph, 'c')).toBe(graph[2]);
  });

  it(`returns undefined if it cannot find a graph that matches the given name`, () => {
    const graph = [{ name: 'a' }, { name: 'b' }, { name: 'c' }];

    expect(findByGraphName(graph, '10')).toBeUndefined();
  });
});

describe('getConnectors()', () => {
  it('returns the correct connectors', () => {
    const connectors = [
      {
        id: '1',
        kind: 'source',
        name: 'sourceName1',
      },
      {
        id: '2',
        kind: 'source',
        name: 'sourceName2',
      },
      {
        id: '3',
        kind: 'sink',
        name: 'sinkName',
      },
      {
        id: '4',
        kind: 'topic',
        name: 'topicName',
      },
      {
        id: '5',
        kind: 'stream',
        name: 'streamAppName',
      },
    ];

    const { sources, sinks, topics, streams } = getConnectors(connectors);

    expect(sources.length).toBe(2);
    expect(sinks.length).toBe(1);
    expect(topics.length).toBe(1);
    expect(streams.length).toBe(1);
  });

  it('returns an empty array if there is no matched', () => {
    const connectors = [{ id: '1', kind: 'Nah' }];
    const { sources, sinks, topics, streams } = getConnectors(connectors);

    expect(sources.length).toBe(0);
    expect(sinks.length).toBe(0);
    expect(topics.length).toBe(0);
    expect(streams.length).toBe(0);
  });
});
