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
  updatePipelineParams,
  removePrevConnector,
} from '../pipelineNewPageUtils';

describe('removePrevConnector()', () => {
  it('removes previous sink connection', () => {
    const rules = {
      a: ['e', 'f'],
      b: ['d'],
    };

    const sinkId = 'f';

    const expected = {
      ...rules,
      a: ['e'],
    };

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
      name: 'abc',
      objects: {},
      rules: {
        a: ['c'],
        b: ['d'],
      },
    };

    const update = {
      id: 'a',
      to: ['g'],
    };

    const sinkId = 'c';

    const expected = { ...pipelines, rules: { a: ['g'], b: ['d'] } };

    expect(updatePipelineParams({ pipelines, update, sinkId })).toEqual(
      expected,
    );
  });

  it('updates the rules when the update includes rules update', () => {
    const pipelines = {
      name: 'abc',
      objects: {},
      rules: {},
    };

    const update = {
      id: 'a',
      to: ['b', 'c'],
    };

    const updateRule = {
      [update.id]: update.to,
    };

    const expected = {
      ...pipelines,
      rules: { ...pipelines.rules, ...updateRule },
    };

    expect(updatePipelineParams({ pipelines, update })).toEqual(expected);
  });
});
