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

import { LOG_LEVEL } from 'const';
import * as brokerApi from 'api/brokerApi';
import fetchBrokerEpic from '../../broker/fetchBrokerEpic';
import * as actions from 'store/actions';
import { getId, getKey } from 'utils/object';
import { entity as brokerEntity } from 'api/__mocks__/brokerApi';
import { brokerInfoEntity } from 'api/__mocks__/inspectApi';

jest.mock('api/brokerApi');
jest.mock('api/inspectApi');

const key = getKey(brokerEntity);
const bkId = getId(brokerEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  jest.restoreAllMocks();
});

it('fetch broker should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a 10s     ';
    const expected = '--a 2999ms u';
    const subs = '    ^-----------';

    const action$ = hot(input, {
      a: {
        type: actions.fetchBroker.TRIGGER,
        payload: key,
      },
    });
    const output$ = fetchBrokerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchBroker.REQUEST,
        payload: {
          brokerId: bkId,
        },
      },
      u: {
        type: actions.fetchBroker.SUCCESS,
        payload: {
          brokerId: bkId,
          entities: {
            brokers: {
              [bkId]: { ...brokerEntity, ...key },
            },
            infos: {
              [bkId]: { ...brokerInfoEntity, ...key },
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

it('fetch broker multiple times within period should get first result', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const anotherKey = { name: 'anotherbk', group: 'newworkspace' };
    const input = '   ^-a 50ms b   ';
    const expected = '--a 2999ms u-';
    const subs = '    ^------------';

    const action$ = hot(input, {
      a: {
        type: actions.fetchBroker.TRIGGER,
        payload: key,
      },
      b: {
        type: actions.fetchBroker.TRIGGER,
        payload: anotherKey,
      },
    });
    const output$ = fetchBrokerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchBroker.REQUEST,
        payload: {
          brokerId: bkId,
        },
      },
      u: {
        type: actions.fetchBroker.SUCCESS,
        payload: {
          brokerId: bkId,
          entities: {
            brokers: {
              [bkId]: { ...brokerEntity, ...key },
            },
            infos: {
              [bkId]: { ...brokerInfoEntity, ...key },
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

it('fetch broker multiple times without period should get latest result', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const anotherKey = { name: 'anotherbk', group: 'newworkspace' };
    const input = '   ^-a 2s b         ';
    const expected = '--a 2s b 2999ms u';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.fetchBroker.TRIGGER,
        payload: key,
      },
      b: {
        type: actions.fetchBroker.TRIGGER,
        payload: anotherKey,
      },
    });
    const output$ = fetchBrokerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchBroker.REQUEST,
        payload: {
          brokerId: bkId,
        },
      },
      b: {
        type: actions.fetchBroker.REQUEST,
        payload: {
          brokerId: getId(anotherKey),
        },
      },
      u: {
        type: actions.fetchBroker.SUCCESS,
        payload: {
          brokerId: getId(anotherKey),
          entities: {
            brokers: {
              [getId(anotherKey)]: { ...brokerEntity, ...anotherKey },
            },
            infos: {
              [getId(anotherKey)]: { ...brokerInfoEntity, ...anotherKey },
            },
          },
          result: getId(anotherKey),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of fetch broker should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock get broker failed',
  };
  const spyCreate = jest
    .spyOn(brokerApi, 'get')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchBroker.TRIGGER,
        payload: brokerEntity,
      },
    });
    const output$ = fetchBrokerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchBroker.REQUEST,
        payload: { brokerId: bkId },
      },
      e: {
        type: actions.fetchBroker.FAILURE,
        payload: { ...error, brokerId: bkId },
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          ...error,
          brokerId: bkId,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyCreate).toHaveBeenCalled();
  });
});
