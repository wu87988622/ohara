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

import {
  isSource,
  isSink,
  isTopic,
  isStream,
  findByGraphName,
  getConnectors,
} from '../commonUtils';

describe('isSource()', () => {
  it(`returns true if it's a source connector`, () => {
    expect(isSource('source')).toBe(true);
  });

  it(`returns false if it's not a source connector`, () => {
    expect(isSource('sink')).toBe(false);
  });
});

describe('isSink()', () => {
  it(`returns true if it's a sink connector`, () => {
    expect(isSink('sink')).toBe(true);
  });

  it(`returns false if it's not a sink connector`, () => {
    expect(isSink('source')).toBe(false);
  });
});

describe('isTopic()', () => {
  it(`returns true if it's a topic`, () => {
    expect(isTopic('topic')).toBe(true);
  });

  it(`returns false if it's not a topic`, () => {
    expect(isTopic('source')).toBe(false);
  });
});

describe('isStream()', () => {
  it(`returns true if it's a stream app`, () => {
    expect(isStream('streamApp')).toBe(true);
  });

  it(`returns false if it's not a topic`, () => {
    expect(isStream('topic')).toBe(false);
  });
});

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
      },
      {
        id: '2',
        kind: 'source',
      },
      {
        id: '3',
        kind: 'sink',
      },
      {
        id: '4',
        kind: 'topic',
      },
      {
        id: '5',
        kind: 'streamApp',
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
