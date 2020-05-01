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
import { of } from 'rxjs';

import stopBrokerEpic from '../../broker/stopBrokerEpic';
import * as brokerApi from 'api/brokerApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import { entity as brokerEntity } from 'api/__mocks__/brokerApi';
import { LOG_LEVEL } from 'const';

jest.mock('api/brokerApi');

const bkId = getId(brokerEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  // ensure the mock data is as expected before each test
  jest.restoreAllMocks();
});

it('stop broker should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 499ms v';
    const subs = '    ^----------';

    const action$ = hot(input, {
      a: {
        type: actions.stopBroker.TRIGGER,
        payload: brokerEntity,
      },
    });
    const output$ = stopBrokerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopBroker.REQUEST,
        payload: {
          brokerId: bkId,
        },
      },
      v: {
        type: actions.stopBroker.SUCCESS,
        payload: {
          brokerId: bkId,
          entities: {
            brokers: {
              [bkId]: brokerEntity,
            },
          },
          result: bkId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('stop broker failed after reach retry limit', () => {
  // mock a 20 times "failed stopped" result
  const spyGet = jest.spyOn(brokerApi, 'get');
  for (let i = 0; i < 20; i++) {
    spyGet.mockReturnValueOnce(
      of({
        status: 200,
        title: 'retry mock get data',
        data: { state: SERVICE_STATE.RUNNING },
      }),
    );
  }
  // get result finally
  spyGet.mockReturnValueOnce(
    of({
      status: 200,
      title: 'retry mock get data',
      data: { ...brokerEntity },
    }),
  );

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a             ';
    // we failed after retry 11 times (11 * 2000ms = 22s)
    const expected = '--a 21999ms (vu)';
    const subs = '    ^---------------';

    const action$ = hot(input, {
      a: {
        type: actions.stopBroker.TRIGGER,
        payload: brokerEntity,
      },
    });
    const output$ = stopBrokerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopBroker.REQUEST,
        payload: {
          brokerId: bkId,
        },
      },
      v: {
        type: actions.stopBroker.FAILURE,
        payload: {
          brokerId: bkId,
          title: 'stop broker exceeded max retry count',
        },
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          brokerId: bkId,
          title: 'stop broker exceeded max retry count',
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('stop broker multiple times should be executed once', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---a 1s a 10s ';
    const expected = '--a       499ms v';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.stopBroker.TRIGGER,
        payload: brokerEntity,
      },
    });
    const output$ = stopBrokerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopBroker.REQUEST,
        payload: {
          brokerId: bkId,
        },
      },
      v: {
        type: actions.stopBroker.SUCCESS,
        payload: {
          brokerId: bkId,
          entities: {
            brokers: {
              [bkId]: brokerEntity,
            },
          },
          result: bkId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('stop different broker should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const anotherBrokerEntity = {
      ...brokerEntity,
      name: 'anotherbk',
      group: 'default',
      xms: 1111,
      xmx: 2222,
      clientPort: 3333,
    };
    const input = '   ^-a--b           ';
    const expected = '--a--b 496ms y--z';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.stopBroker.TRIGGER,
        payload: brokerEntity,
      },
      b: {
        type: actions.stopBroker.TRIGGER,
        payload: anotherBrokerEntity,
      },
    });
    const output$ = stopBrokerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopBroker.REQUEST,
        payload: {
          brokerId: bkId,
        },
      },
      b: {
        type: actions.stopBroker.REQUEST,
        payload: {
          brokerId: getId(anotherBrokerEntity),
        },
      },
      y: {
        type: actions.stopBroker.SUCCESS,
        payload: {
          brokerId: bkId,
          entities: {
            brokers: {
              [bkId]: brokerEntity,
            },
          },
          result: bkId,
        },
      },
      z: {
        type: actions.stopBroker.SUCCESS,
        payload: {
          brokerId: getId(anotherBrokerEntity),
          entities: {
            brokers: {
              [getId(anotherBrokerEntity)]: anotherBrokerEntity,
            },
          },
          result: getId(anotherBrokerEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
