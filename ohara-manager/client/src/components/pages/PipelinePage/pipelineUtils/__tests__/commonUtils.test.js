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
  findByGraphId,
  getConnectors,
  updateTopic,
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

describe('findByGraphId()', () => {
  it('finds the right connector by ID', () => {
    const graph = [
      { id: '1', name: 'a' },
      { id: '2', name: 'b' },
      { id: '3', name: 'c' },
    ];
    expect(findByGraphId(graph, '3')).toBe(graph[2]);
  });

  it(`returns undefined if it cannot find a graph that matches the given id`, () => {
    const graph = [
      { id: '1', name: 'a' },
      { id: '2', name: 'b' },
      { id: '3', name: 'c' },
    ];

    expect(findByGraphId(graph, '10')).toBeUndefined();
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

describe('updateTopic()', () => {
  it('updates sources', () => {
    const props = {
      graph: [
        {
          id: '1',
          kind: 'topic',
          name: 'a',
          to: [],
        },
        {
          id: '2',
          kind: 'source',
          name: 'b',
          to: [],
        },
      ],
      match: {
        params: {
          connectorId: '2',
        },
      },
      updateGraph: jest.fn(),
    };

    const currTopic = { id: '1', name: 't', to: ['1'] };
    const connectorType = 'source';

    updateTopic(props, currTopic, connectorType);

    const update = { ...props.graph[1], to: currTopic.to };

    expect(props.updateGraph).toHaveBeenCalledTimes(1);
    expect(props.updateGraph).toHaveBeenCalledWith({ update });
  });

  it('updates sinks', () => {
    const props = {
      graph: [
        {
          id: '1',
          kind: 'topic',
          name: 'a',
          to: [],
        },
        {
          id: '2',
          kind: 'source',
          name: 'b',
          to: [],
        },
      ],
      match: {
        params: {
          connectorId: '2',
        },
      },
      updateGraph: jest.fn(),
    };

    const currTopic = { id: '1', name: 't', to: ['1'] };
    const connectorType = 'sink';

    updateTopic(props, currTopic, connectorType);

    const update = {
      ...props.graph[0],
      to: [props.match.params.connectorId],
    };

    expect(props.updateGraph).toHaveBeenCalledTimes(1);
    expect(props.updateGraph).toHaveBeenCalledWith({
      update,
      isSinkUpdate: true,
    });
  });

  it(`returns undefined if currTopic is not defined`, () => {
    expect(updateTopic()).toBeUndefined();
  });

  it('returns undefined if currTopic is an empty object', () => {
    const props = {};
    const currTopic = {};
    const connectorType = 'sink';
    expect(updateTopic(props, currTopic, connectorType)).toBeUndefined();
  });
});
