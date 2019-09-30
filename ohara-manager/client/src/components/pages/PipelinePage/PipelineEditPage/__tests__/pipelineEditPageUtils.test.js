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

import * as generate from 'utils/generate';
import * as utils from '../pipelineEditPageUtils';

describe('removePrevConnector()', () => {
  it('removes previous connection', () => {
    const connectorNameOne = generate.name();
    const connectorNameTwo = generate.name();
    const flows = [
      {
        form: { group: 'default', name: generate.word() },
        to: [
          { group: 'default', name: connectorNameOne },
          {
            group: 'default',
            name: connectorNameTwo,
          },
        ],
      },
    ];

    const expected = [
      {
        ...flows[0],
        to: [{ group: 'default', name: connectorNameTwo }],
      },
    ];

    expect(utils.removePrevConnector(flows, connectorNameOne)).toEqual(
      expected,
    );
  });
});

describe('updatePipelineFlows()', () => {
  it('updates params correctly', () => {
    const group = generate.word();

    const pipeline = {
      name: 'abc',
      objects: {},
      group,
      flows: [
        {
          from: { group, name: 'abc' },
          to: [],
        },
        {
          from: { group, name: 'efg' },
          to: [],
        },
      ],
      tags: {
        workerClusterName: 'e',
      },
    };

    const update = {
      update: { name: 'abc', to: ['efg'] },
      dispatcher: { name: 'CONNECTOR' },
    };

    const expected = [
      {
        from: { group, name: 'abc' },
        to: [{ group, name: 'efg' }],
      },
      {
        from: { group, name: 'efg' },
        to: [],
      },
    ];

    expect(utils.updateFlows({ pipeline, ...update })).toEqual(expected);
  });
});

describe('updateSingleGraph()', () => {
  it('updates the target graph', () => {
    const graph = [{ name: 'a' }, { name: 'b' }];

    const updated = utils.updateSingleGraph(graph, 'a', g => {
      return { ...g, name: 'new name' };
    });

    expect(updated[0].name).toBe('new name');
  });
});

describe('cleanPrevFromTopics', () => {
  it('removes the previous topic', () => {
    const graph = [
      {
        name: 'a',
        to: ['2', '3', '4'],
      },
      {
        name: 'b',
        to: ['5'],
      },
    ];

    const expected = [
      {
        name: 'a',
        to: ['3', '4'],
      },
      {
        name: 'b',
        to: ['5'],
      },
    ];

    expect(utils.cleanPrevFromTopics(graph, '2')).toEqual(expected);
  });

  it(`returns the given graph if there's no matched of previous topic`, () => {
    const graph = [
      {
        id: '1',
        name: 'a',
        to: ['2', '3', '4'],
      },
      {
        id: '2',
        name: 'b',
        to: ['5'],
      },
    ];
    expect(utils.cleanPrevFromTopics(graph, '1')).toEqual(graph);
  });
});

describe('updateGraph()', () => {
  it(`Adds a new connector to the current graph if it's not being found in the current graph`, () => {
    const graph = [{ name: 'a', to: ['b'], isActive: false }];
    const update = { name: 'b', to: [] };
    const dispatcher = { name: 'NOT_TOOLBAR' };

    const expected = [...graph, { ...update, isActive: true }];

    expect(utils.updateGraph({ graph, update, dispatcher })).toEqual(expected);
  });

  it(`Should not update the connector active state if the update is coming from toolbar`, () => {
    const graph = [{ name: 'a', to: [], isActive: true }];
    const update = { name: 'b', to: [], isActive: false };
    const dispatcher = { name: 'TOOLBAR' };

    const expected = [...graph, update];

    expect(utils.updateGraph({ graph, update, dispatcher })).toEqual(expected);
  });

  it(`updates the correct connector in the graph`, () => {
    const graph = [{ name: 'a', to: [] }];
    const update = { name: 'a', to: ['b'] };
    const dispatcher = { name: 'NOT_TOOLBAR' };

    const expected = [{ name: 'a', to: ['b'], isActive: true }];

    expect(utils.updateGraph({ graph, update, dispatcher })).toEqual(expected);
  });

  it('updates formTopic and sink connectors', () => {
    const graph = [
      {
        name: 'a',
        to: ['b', 'c', 'd'],
      },
      {
        name: 'test',
        to: ['e'],
      },
    ];

    const sinkName = 'abc';
    const update = { name: 'c', to: ['e'] };
    const dispatcher = { name: 'NOT_TOOLBAR' };

    const result = utils.updateGraph({
      graph,
      update,
      sinkName,
      isFromTopic: true,
      dispatcher,
    });

    const expected = [
      {
        name: 'a',
        to: ['b', 'c', 'd'],
        isActive: false,
      },
      {
        name: 'test',
        to: ['e'],
        isActive: false,
      },
    ];

    expect(result).toEqual(expected);
  });
});

describe('loadGraph()', () => {
  it('creates the correct data structure', () => {
    const pipeline = {
      objects: [
        { name: 'a', kind: 'source' },
        { name: 'b', kind: 'sink' },
        { name: 'c', kind: 'topic' },
        {
          name: 'd',
          kind: 'stream',
          to: [],
        },
      ],
      flows: [
        {
          from: { group: 'default', name: 'a' },
          to: [{ group: 'default', name: 'c' }],
        },
        {
          from: { group: 'default', name: 'c' },
          to: [{ group: 'default', name: 'b' }],
        },
        {
          from: { group: 'default', name: 'b' },
          to: [],
        },
        {
          from: { group: 'default', name: 'd' },
          to: [],
        },
      ],
    };
    const connectorName = 'a';

    const expected = [
      {
        name: 'a',
        kind: 'source',
        to: [{ group: 'default', name: 'c' }],
        isActive: true,
      },
      {
        name: 'c',
        kind: 'topic',
        className: 'topic',
        to: [{ group: 'default', name: 'b' }],
        isActive: false,
      },
      { name: 'b', kind: 'sink', to: [], isActive: false },
      {
        name: 'd',
        kind: 'stream',
        className: 'stream',
        to: [],
        isActive: false,
      },
    ];

    expect(utils.loadGraph(pipeline, connectorName)).toEqual(expected);
  });

  it('should not create a graph object when the target object is not found in the flows field', () => {
    const pipeline = {
      flows: [
        {
          from: { group: 'default', name: 'abc' },
          to: [],
        },
      ],
      objects: [],
    };

    expect(utils.loadGraph(pipeline, '123')).toEqual([]);
  });

  it('should filter out not supported object kinds', () => {
    const pipeline = {
      flows: [
        {
          from: {
            group: 'default',
            name: 'abc',
          },
          to: [],
        },
        {
          from: {
            group: 'default',
            name: 'cdf',
          },
          to: [],
        },
      ],
      objects: [
        {
          name: 'abc',
          kind: 'xxx',
        },
        {
          name: 'cdf',
          kind: 'topic',
        },
      ],
    };

    const expected = [
      {
        className: 'topic',
        isActive: true,
        kind: 'topic',
        name: 'cdf',
        to: [],
      },
    ];

    expect(utils.loadGraph(pipeline, 'cdf')).toEqual(expected);
  });

  it('should add class name to stream and topic objects', () => {
    const pipeline = {
      flows: [
        {
          from: {
            group: 'default',
            name: 'abc',
          },
          to: [],
        },
        {
          from: {
            group: 'default',
            name: 'cdf',
          },
          to: [],
        },
        {
          from: {
            group: 'default',
            name: 'kfs',
          },
          to: [],
        },
      ],
      objects: [
        {
          name: 'abc',
          kind: 'stream',
        },
        {
          name: 'cdf',
          kind: 'topic',
        },
        {
          name: 'kfs',
          kind: 'source',
          className: 'unknown',
        },
      ],
    };

    const expected = [
      {
        className: 'stream',
        isActive: true,
        kind: 'stream',
        name: 'abc',
        to: [],
      },
      {
        className: 'topic',
        isActive: false,
        kind: 'topic',
        name: 'cdf',
        to: [],
      },
      {
        className: 'unknown',
        isActive: false,
        kind: 'source',
        name: 'kfs',
        to: [],
      },
    ];

    expect(utils.loadGraph(pipeline, 'abc')).toEqual(expected);
  });
});
