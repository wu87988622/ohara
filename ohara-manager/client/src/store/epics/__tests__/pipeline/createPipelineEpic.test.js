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

import { throwError } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import * as actions from 'store/actions';
import * as pipelineApi from 'api/pipelineApi';
import createPipelineEpic from '../../pipeline/createPipelineEpic';
import { getId } from 'utils/object';
import { entity as pipelineEntity } from 'api/__mocks__/pipelineApi';
import { LOG_LEVEL } from 'const';

jest.mock('api/pipelineApi');

const pipelineId = getId(pipelineEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('should create a new pipeline and switch to it', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a         ';
    const expected = '--a 99ms (bc)';
    const subs = '    ^-----------';

    const action$ = hot(input, {
      a: {
        type: actions.createPipeline.TRIGGER,
        payload: { group: pipelineEntity.group, name: pipelineEntity.name },
      },
    });
    const output$ = createPipelineEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createPipeline.REQUEST,
      },
      b: {
        type: actions.createPipeline.SUCCESS,
        payload: {
          result: pipelineId,
          entities: {
            pipelines: {
              [pipelineId]: pipelineEntity,
            },
          },
        },
      },
      c: {
        type: actions.switchPipeline.TRIGGER,
        payload: { name: pipelineEntity.name },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('should handle error', () => {
  const error = {
    status: -1,
    data: {},
    title: 'Create pipeline failure mock',
  };

  const spyCreate = jest
    .spyOn(pipelineApi, 'create')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.createPipeline.TRIGGER,
        payload: { group: pipelineEntity.group, name: pipelineEntity.name },
      },
    });
    const output$ = createPipelineEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createPipeline.REQUEST,
      },
      e: {
        type: actions.createPipeline.FAILURE,
        payload: error,
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          ...error,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyCreate).toHaveBeenCalledTimes(1);
  });
});
