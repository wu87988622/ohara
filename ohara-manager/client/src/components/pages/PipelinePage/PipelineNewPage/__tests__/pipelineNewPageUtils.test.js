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
  cleanPrevFromTopics,
  removePrevConnector,
  updatePipelineParams,
  updateSingleGraph,
  updateGraph,
  loadGraph,
} from '../pipelineNewPageUtils';

describe('removePrevConnector()', () => {
  it('removes previous sink connection', () => {
    const rules = { a: ['e', 'f'], b: ['d'] };
    const sinkId = 'f';
    const expected = { ...rules, a: ['e'] };

    expect(removePrevConnector(rules, sinkId)).toEqual(expected);
  });
});

describe('updatePipelineparams()', () => {
  it('returns the pipeline if the update is only for the pipeline name', () => {
    const pipeline = {
      name: 'abc',
      objects: {},
      rules: {},
    };

    expect(updatePipelineParams({ pipeline })).toBe(pipeline);
  });

  it('updates params correctly', () => {
    const pipeline = {
      name: 'abc',
      objects: {},
      rules: {
        a: ['c'],
        b: ['d'],
      },
      workerClusterName: 'e',
    };
    const update = { name: 'a', to: ['g'] };
    const sinkName = 'c';
    const expected = {
      name: pipeline.name,
      workerClusterName: pipeline.workerClusterName,
      rules: { a: ['g'], b: ['d'] },
    };

    expect(updatePipelineParams({ pipeline, update, sinkName })).toEqual(
      expected,
    );
  });

  it('updates the rules when the update includes rules update', () => {
    const pipeline = {
      name: 'abc',
      objects: {},
      rules: {},
      workerClusterName: 'e',
    };
    const update = { name: 'a', to: ['b', 'c'] };
    const updateRule = {
      [update.name]: update.to,
    };
    const expected = {
      name: pipeline.name,
      workerClusterName: pipeline.workerClusterName,
      rules: { ...pipeline.rules, ...updateRule },
    };

    expect(updatePipelineParams({ pipeline, update })).toEqual(expected);
  });
});

describe('updateSingleGraph()', () => {
  it('updates the target graph', () => {
    const graph = [{ name: 'a' }, { name: 'b' }];

    const updated = updateSingleGraph(graph, 'a', g => {
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

    expect(cleanPrevFromTopics(graph, '2')).toEqual(expected);
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
    expect(cleanPrevFromTopics(graph, '1')).toEqual(graph);
  });
});

describe('updateGraph()', () => {
  it(`Adds a new connector to the current graph if it's not being found in the current graph`, () => {
    const graph = [{ name: 'a', to: ['b'] }];
    const update = { name: 'b', to: [] };
    const expected = [...graph, update];

    expect(updateGraph({ graph, update })).toEqual(expected);
  });

  it(`updates the correct connector in the graph`, () => {
    const graph = [{ name: 'a', to: [] }];
    const update = { name: 'a', to: ['b'] };
    const expected = [{ ...graph[0], ...update }];

    expect(updateGraph({ graph, update })).toEqual(expected);
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
    const updatedName = 'test';
    const update = { name: 'c', to: ['e'] };

    const result = updateGraph({
      graph,
      update,
      updatedName,
      sinkName,
      isFromTopic: true,
    });

    const expected = [
      {
        name: 'a',
        to: ['b', 'c', 'd'],
      },
      {
        name: 'test',
        to: ['e'],
      },
    ];

    expect(result).toEqual(expected);
  });
});

describe('loadGraph()', () => {
  it('creates the correct data structure', () => {
    const pipeline = {
      objects: [
        { name: 'a', kind: 'Source' },
        { name: 'b', kind: 'Sink' },
        { name: 'c', kind: 'topic' },
        {
          name: 'd',
          kind: 'streamApp',
          to: [],
        },
      ],
      rules: { a: ['c'], c: ['b'], b: [], d: [] },
    };
    const connectorName = 'a';

    const expected = [
      { name: 'a', kind: 'Source', to: ['c'], isActive: true },
      {
        name: 'c',
        kind: 'topic',
        className: 'topic',
        to: ['b'],
        isActive: false,
      },
      { name: 'b', kind: 'Sink', to: [], isActive: false },
      {
        name: 'd',
        kind: 'streamApp',
        className: 'streamApp',
        to: [],
        isActive: false,
      },
    ];

    expect(loadGraph(pipeline, connectorName)).toEqual(expected);
  });
});
