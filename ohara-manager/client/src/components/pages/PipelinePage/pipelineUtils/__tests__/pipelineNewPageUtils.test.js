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
    const pipelines = {
      name: 'abc',
      objects: {},
      rules: {},
    };

    expect(updatePipelineParams({ pipelines })).toBe(pipelines);
  });

  it('updates params correctly', () => {
    const pipelines = {
      id: '123',
      name: 'abc',
      objects: {},
      rules: {
        a: ['c'],
        b: ['d'],
      },
      status: 'failed',
      workerClusterName: 'e',
    };
    const update = { id: 'a', to: ['g'] };
    const sinkId = 'c';
    const expected = {
      id: pipelines.id,
      name: pipelines.name,
      status: pipelines.status,
      workerClusterName: pipelines.workerClusterName,
      rules: { a: ['g'], b: ['d'] },
    };

    expect(updatePipelineParams({ pipelines, update, sinkId })).toEqual(
      expected,
    );
  });

  it('updates the rules when the update includes rules update', () => {
    const pipelines = {
      id: '123',
      name: 'abc',
      objects: {},
      rules: {},
      status: 'success',
      workerClusterName: 'e',
    };
    const update = { id: 'a', to: ['b', 'c'] };
    const updateRule = {
      [update.id]: update.to,
    };
    const expected = {
      id: pipelines.id,
      name: pipelines.name,
      status: pipelines.status,
      workerClusterName: pipelines.workerClusterName,
      rules: { ...pipelines.rules, ...updateRule },
    };

    expect(updatePipelineParams({ pipelines, update })).toEqual(expected);
  });
});

describe('updateSingleGraph()', () => {
  it('updates the target graph', () => {
    const graph = [{ id: '1', name: 'a' }, { id: '2', name: 'b' }];

    const updated = updateSingleGraph(graph, '1', g => {
      return { ...g, name: 'new name' };
    });

    expect(updated[0].name).toBe('new name');
  });
});

describe('cleanPrevFromTopics', () => {
  it('removes the previous topic', () => {
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

    const expected = [
      {
        id: '1',
        name: 'a',
        to: ['3', '4'],
      },
      {
        id: '2',
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
    const graph = [{ id: '1', name: 'a', to: ['2'] }];
    const update = { id: '2', name: 'b', to: [] };
    const expected = [...graph, update];

    expect(updateGraph({ graph, update })).toEqual(expected);
  });

  it(`updates the correct connector in the graph`, () => {
    const graph = [{ id: '1', name: 'a', to: [] }];
    const update = { id: '1', name: 'test', to: ['2', '3', '4'] };
    const expected = [{ ...graph[0], ...update }];

    expect(updateGraph({ graph, update })).toEqual(expected);
  });

  it('updates formTopic and sink connectors', () => {
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

    const sinkId = '2';
    const updatedName = 'test';
    const update = { id: '3', name: 'c', to: ['5'] };

    const result = updateGraph({
      graph,
      update,
      updatedName,
      sinkId,
      isFromTopic: true,
    });

    const expected = [
      {
        id: '1',
        name: 'a',
        to: ['3', '4'],
      },
      {
        id: '2',
        name: 'test',
        to: ['5'],
      },
    ];

    expect(result).toEqual(expected);
  });
});

describe('loadGraph()', () => {
  it('creates the correct data structure', () => {
    const pipelines = {
      objects: [
        { id: '1', name: 'a', kind: 'Source' },
        { id: '2', name: 'b', kind: 'Sink' },
        { id: '3', name: 'c', kind: 'topic' },
        {
          id: '4',
          name: 'd',
          kind: 'streamApp',
          to: [],
        },
      ],
      rules: { '1': ['3'], '3': ['2'], '2': [], 4: [] },
    };

    const expected = [
      { id: '1', name: 'a', kind: 'Source', to: ['3'] },
      { id: '2', name: 'b', kind: 'Sink', to: [] },
      { id: '3', name: 'c', kind: 'topic', className: 'topic', to: ['2'] },
      {
        id: '4',
        name: 'd',
        kind: 'streamApp',
        className: 'streamApp',
        to: [],
      },
    ];

    expect(loadGraph(pipelines)).toEqual(expected);
  });
});
