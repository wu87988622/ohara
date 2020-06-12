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
import { delay } from 'rxjs/operators';
import { of, throwError } from 'rxjs';

import * as actions from 'store/actions';
import * as pipelineApi from 'api/pipelineApi';
import updatePipelineMetricsEpic from '../../pipeline/updatePipelineMetricsEpic';
import { entity as pipelineEntity } from 'api/__mocks__/pipelineApi';
import { KIND } from 'const';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import { LOG_LEVEL } from 'const';

jest.mock('api/pipelineApi');

const paperApi = {
  updateMetrics: jest.fn(),
};

beforeEach(() => {
  jest.restoreAllMocks();
  jest.resetAllMocks();
});

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('should start to update pipeline metrics and stop when a stop action is dispatched', () => {
  const objects = [
    {
      kind: KIND.topic,
      state: SERVICE_STATE.RUNNING,
      nodeMetrics: {
        'ohara-dev-node-01': { meters: [] },
      },
    },
    {
      kind: KIND.source,
      state: SERVICE_STATE.RUNNING,
      nodeMetrics: {
        'ohara-dev-node-01': { meters: [] },
      },
    },
  ];

  jest.spyOn(pipelineApi, 'get').mockReturnValue(
    of({
      status: 200,
      title: 'Get pipeline mock',
      data: {
        ...pipelineEntity,
        objects,
      },
    }).pipe(delay(6)),
  );

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, flush, expectSubscriptions } = helpers;

    const input = '   ^-a 20000ms                    (k|)';
    const expected = '-- 6ms a 7999ms a 7999ms a 3994ms |';
    const subs = ['^ 20002ms !', '--^ 20000ms !'];

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
        type: actions.startUpdateMetrics.SUCCESS,
        payload: objects,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(paperApi.updateMetrics).toHaveBeenCalledTimes(3);
    expect(paperApi.updateMetrics).toHaveBeenCalledWith(objects);
  });
});

it('should not trigger any actions when no running services found in the response', () => {
  const objects = [
    {
      kind: KIND.topic,
      state: SERVICE_STATE.RUNNING,
      nodeMetrics: {
        'ohara-dev-node-01': { meters: [] },
      },
    },
    {
      kind: KIND.source,
      state: SERVICE_STATE.RUNNING,
      nodeMetrics: {
        'ohara-dev-node-01': { meters: [] },
      },
    },
  ];

  jest.spyOn(pipelineApi, 'get').mockReturnValue(
    of({
      status: 200,
      title: 'Get pipeline mock',
      data: {
        ...pipelineEntity,
        objects: [],
      },
    }).pipe(delay(6)),
  );

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, flush } = helpers;

    const input = '   ^-a 10000ms (k|)';
    const expected = '--- 10000ms    |';

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
        type: actions.startUpdateMetrics.SUCCESS,
        payload: objects,
      },
    });

    flush();

    expect(paperApi.updateMetrics).toHaveBeenCalledTimes(0);
  });
});

// TODO: Add new tests to test all of the actions that are able to stop this epic
it('should stop the epic when a stop action is fired', () => {
  const objects = [
    {
      kind: KIND.topic,
      state: SERVICE_STATE.RUNNING,
      nodeMetrics: {
        'ohara-dev-node-01': { meters: [] },
      },
    },
    {
      kind: KIND.source,
      state: SERVICE_STATE.RUNNING,
      nodeMetrics: {
        'ohara-dev-node-01': { meters: [] },
      },
    },
  ];

  jest.spyOn(pipelineApi, 'get').mockReturnValue(
    of({
      status: 200,
      title: 'Get pipeline mock',
      data: {
        ...pipelineEntity,
        objects,
      },
    }).pipe(delay(6)),
  );

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, flush, expectSubscriptions } = helpers;

    const input = '   ^-a 10000ms           (k|)';
    const expected = '-- 6ms a 7999ms a 1994ms |';
    const subs = ['^ 10002ms !', '--^ 10000ms !'];

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
        type: actions.startUpdateMetrics.SUCCESS,
        payload: objects,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(paperApi.updateMetrics).toHaveBeenCalledTimes(2);
    expect(paperApi.updateMetrics).toHaveBeenCalledWith(objects);
  });
});

it('should handle error', () => {
  const error = {
    status: -1,
    title: 'Get pipeline mock',
    data: {},
  };

  jest.spyOn(pipelineApi, 'get').mockReturnValue(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, flush, expectSubscriptions } = helpers;

    const input = '   ^a---| ';
    const expected = '-(eu)|';
    const subs = ['^----!', '-(^!)'];

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
          },
        },
      },
    });

    const output$ = updatePipelineMetricsEpic(action$);

    expectObservable(output$).toBe(expected, {
      e: {
        type: actions.startUpdateMetrics.FAILURE,
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

    expect(paperApi.updateMetrics).toHaveBeenCalledTimes(0);
  });
});
