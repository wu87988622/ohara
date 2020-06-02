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

import { TestScheduler } from 'rxjs/testing';

import * as actions from 'store/actions';
import updatePipelineMetricsEpic from '../../pipeline/updatePipelineMetricsEpic';
import { entity as pipelineEntity } from 'api/__mocks__/pipelineApi';

jest.mock('api/pipelineApi');

const paperApi = {
  updateMetrics: jest.fn(),
};

beforeEach(jest.resetAllMocks);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('should start to update pipeline metrics and stop when a stop action is dispatched', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, flush } = helpers;

    const input = '   ^-a 20000ms                                 (k|)';
    const expected = '--a 5ms b 7993ms a 5ms b 7993ms a 5ms b 3994ms |';

    const action$ = hot(input, {
      a: {
        type: actions.startUpdateMetrics.TRIGGER,
        payload: {
          params: {
            group: pipelineEntity.group,
            name: pipelineEntity.name,
          },
          options: {
            paperApi,
            pipelineObjectsRef: { current: null },
          },
        },
      },
      k: {
        type: actions.stopUpdateMetrics.TRIGGER,
      },
    });

    const output$ = updatePipelineMetricsEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startUpdateMetrics.REQUEST,
      },
      b: {
        type: actions.startUpdateMetrics.SUCCESS,
      },
    });

    flush();

    expect(paperApi.updateMetrics).toHaveBeenCalledTimes(3);
  });
});

it('should terminate the request if an error occurs', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, flush } = helpers;

    const input = '   ^-a 10000ms                 (k|)';
    const expected = '--a 5ms b 7993ms a 5ms b 1994ms |';

    const action$ = hot(input, {
      a: {
        type: actions.startUpdateMetrics.TRIGGER,
        payload: {
          params: {
            group: pipelineEntity.group,
            name: pipelineEntity.name,
          },
          options: {
            paperApi,
            pipelineObjectsRef: { current: null },
          },
        },
      },
      k: {
        type: actions.startUpdateMetrics.FAILURE,
      },
    });

    const output$ = updatePipelineMetricsEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startUpdateMetrics.REQUEST,
      },
      b: {
        type: actions.startUpdateMetrics.SUCCESS,
      },
    });

    flush();

    expect(paperApi.updateMetrics).toHaveBeenCalledTimes(2);
  });
});
