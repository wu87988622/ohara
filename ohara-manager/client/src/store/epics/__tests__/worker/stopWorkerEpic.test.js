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

import { LOG_LEVEL } from 'const';
import * as workerApi from 'api/workerApi';
import stopWorkerEpic from '../../worker/stopWorkerEpic';
import { entity as workerEntity } from 'api/__mocks__/workerApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { of } from 'rxjs';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';

jest.mock('api/workerApi');

const wkId = getId(workerEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  // ensure the mock data is as expected before each test
  jest.restoreAllMocks();
});

it('stop worker should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 499ms v';
    const subs = ['   ^----------', '--^ 499ms !'];

    const action$ = hot(input, {
      a: {
        type: actions.stopWorker.TRIGGER,
        payload: { values: workerEntity },
      },
    });
    const output$ = stopWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopWorker.REQUEST,
        payload: {
          workerId: wkId,
        },
      },
      v: {
        type: actions.stopWorker.SUCCESS,
        payload: {
          workerId: wkId,
          entities: {
            workers: {
              [wkId]: workerEntity,
            },
          },
          result: wkId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('stop worker failed after reach retry limit', () => {
  // mock a 20 times "failed stopped" results
  const spyGet = jest.spyOn(workerApi, 'get');
  for (let i = 0; i < 20; i++) {
    spyGet.mockReturnValueOnce(
      of({
        status: 200,
        title: 'retry mock get data',
        data: { ...workerEntity, state: SERVICE_STATE.RUNNING },
      }),
    );
  }
  // get result finally
  spyGet.mockReturnValueOnce(
    of({
      status: 200,
      title: 'retry mock get data',
      data: { ...workerEntity },
    }),
  );

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a             ';
    // we failed after retry 10 times (10 * 2000ms = 20s)
    const expected = '--a 19999ms (vu)';
    const subs = ['   ^---------------', '--^ 19999ms !'];

    const action$ = hot(input, {
      a: {
        type: actions.stopWorker.TRIGGER,
        payload: { values: workerEntity },
      },
    });
    const output$ = stopWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopWorker.REQUEST,
        payload: {
          workerId: wkId,
        },
      },
      v: {
        type: actions.stopWorker.FAILURE,
        payload: {
          workerId: wkId,
          data: { ...workerEntity, state: SERVICE_STATE.RUNNING },
          meta: undefined,
          title: `Try to stop worker: "${workerEntity.name}" failed after retry 10 times. Expected state is nonexistent, Actual state: RUNNING`,
        },
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          workerId: wkId,
          data: { ...workerEntity, state: SERVICE_STATE.RUNNING },
          meta: undefined,
          title: `Try to stop worker: "${workerEntity.name}" failed after retry 10 times. Expected state is nonexistent, Actual state: RUNNING`,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('stop worker multiple times should be executed once', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---a 1s a 10s ';
    const expected = '--a       499ms v';
    const subs = ['   ^----------------', '--^ 499ms !'];

    const action$ = hot(input, {
      a: {
        type: actions.stopWorker.TRIGGER,
        payload: { values: workerEntity },
      },
    });
    const output$ = stopWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopWorker.REQUEST,
        payload: {
          workerId: wkId,
        },
      },
      v: {
        type: actions.stopWorker.SUCCESS,
        payload: {
          workerId: wkId,
          entities: {
            workers: {
              [wkId]: workerEntity,
            },
          },
          result: wkId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('stop different worker should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const anotherWorkerEntity = {
      ...workerEntity,
      name: 'anotherwk',
      group: 'default',
      xms: 1111,
      xmx: 2222,
      clientPort: 3333,
    };
    const input = '   ^-a--b           ';
    const expected = '--a--b 496ms y--z';
    const subs = ['   ^----------------', '--^ 499ms !', '-----^ 499ms !'];

    const action$ = hot(input, {
      a: {
        type: actions.stopWorker.TRIGGER,
        payload: { values: workerEntity },
      },
      b: {
        type: actions.stopWorker.TRIGGER,
        payload: { values: anotherWorkerEntity },
      },
    });
    const output$ = stopWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopWorker.REQUEST,
        payload: {
          workerId: wkId,
        },
      },
      b: {
        type: actions.stopWorker.REQUEST,
        payload: {
          workerId: getId(anotherWorkerEntity),
        },
      },
      y: {
        type: actions.stopWorker.SUCCESS,
        payload: {
          workerId: wkId,
          entities: {
            workers: {
              [wkId]: workerEntity,
            },
          },
          result: wkId,
        },
      },
      z: {
        type: actions.stopWorker.SUCCESS,
        payload: {
          workerId: getId(anotherWorkerEntity),
          entities: {
            workers: {
              [getId(anotherWorkerEntity)]: anotherWorkerEntity,
            },
          },
          result: getId(anotherWorkerEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
