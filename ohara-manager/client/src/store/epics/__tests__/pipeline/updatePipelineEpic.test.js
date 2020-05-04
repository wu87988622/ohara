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

import updatePipelineEpic from '../../pipeline/updatePipelineEpic';
import * as pipelineApi from 'api/pipelineApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as pipelineEntity } from 'api/__mocks__/pipelineApi';
import { LOG_LEVEL } from 'const';

const pipelineId = getId(pipelineEntity);

jest.mock('api/pipelineApi');

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('should update pipeline without errors', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.updatePipeline.TRIGGER,
        payload: { group: pipelineEntity.group, name: pipelineEntity.name },
      },
    });
    const output$ = updatePipelineEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updatePipeline.REQUEST,
      },
      u: {
        type: actions.updatePipeline.SUCCESS,
        payload: {
          result: pipelineId,
          entities: {
            pipelines: {
              [pipelineId]: pipelineEntity,
            },
          },
        },
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
    title: 'Update pipeline failure mock',
  };

  const spyCreate = jest
    .spyOn(pipelineApi, 'update')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.updatePipeline.TRIGGER,
        payload: { group: pipelineEntity.group, name: pipelineEntity.name },
      },
    });
    const output$ = updatePipelineEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updatePipeline.REQUEST,
      },
      e: {
        type: actions.updatePipeline.FAILURE,
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
