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

const updatePipelineMetrics = jest.fn();

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

    const input = '   ^-a 20000ms (k|)';
    const expected = '--  20001ms    |';
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
            updatePipelineMetrics,
          },
        },
      },
      k: {
        type: actions.stopUpdateMetrics.TRIGGER,
      },
    });

    const output$ = updatePipelineMetricsEpic(action$);

    expectObservable(output$).toBe(expected);

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    // We fetch the data upon every 8 seconds, and the first time is fetched
    // on epic startup, so there are 3 times of function calls 👇
    expect(updatePipelineMetrics).toHaveBeenCalledTimes(3);
    expect(updatePipelineMetrics).toHaveBeenCalledWith(objects);
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
            updatePipelineMetrics,
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

    expect(updatePipelineMetrics).toHaveBeenCalledTimes(0);
  });
});

// These actions should all stop this epic from running
it.each([
  actions.switchPipeline.TRIGGER,
  actions.deletePipeline.SUCCESS,
  actions.stopUpdateMetrics.TRIGGER,
  actions.startUpdateMetrics.FAILURE,
])('should stop the epic when a (%s) action is fired', (actionType) => {
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

    const input = '   ^-a 10000ms (k|)';
    const expected = '--  10001ms    |';
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
            updatePipelineMetrics,
          },
        },
      },
      k: {
        type: actionType,
      },
    });

    const output$ = updatePipelineMetricsEpic(action$);

    expectObservable(output$).toBe(expected);

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(updatePipelineMetrics).toHaveBeenCalledTimes(2);
    expect(updatePipelineMetrics).toHaveBeenCalledWith(objects);
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
            updatePipelineMetrics,
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

    expect(updatePipelineMetrics).toHaveBeenCalledTimes(0);
  });
});
