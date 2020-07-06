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
import updateBrokerEpic from '../../broker/updateBrokerEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as brokerEntity } from 'api/__mocks__/brokerApi';

jest.mock('api/brokerApi');

const bkId = getId(brokerEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('update broker should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.updateBroker.TRIGGER,
        payload: { values: { ...brokerEntity, jmxPort: 999 } },
      },
    });
    const output$ = updateBrokerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateBroker.REQUEST,
        payload: {
          brokerId: bkId,
        },
      },
      u: {
        type: actions.updateBroker.SUCCESS,
        payload: {
          brokerId: bkId,
          entities: {
            brokers: {
              [bkId]: { ...brokerEntity, jmxPort: 999 },
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

it('update broker multiple times should got latest result', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 60ms c 10s            ';
    const expected = '--a-b 60ms d 36ms u-v 60ms w';
    const subs = '    ^---------------------------';

    const action$ = hot(input, {
      a: {
        type: actions.updateBroker.TRIGGER,
        payload: { values: { ...brokerEntity } },
      },
      b: {
        type: actions.updateBroker.TRIGGER,
        payload: { values: { ...brokerEntity, nodeNames: ['n1', 'n2'] } },
      },
      c: {
        type: actions.updateBroker.TRIGGER,
        payload: { values: { ...brokerEntity, clientPort: 1234 } },
      },
    });
    const output$ = updateBrokerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateBroker.REQUEST,
        payload: {
          brokerId: bkId,
        },
      },
      b: {
        type: actions.updateBroker.REQUEST,
        payload: {
          brokerId: bkId,
        },
      },
      d: {
        type: actions.updateBroker.REQUEST,
        payload: {
          brokerId: bkId,
        },
      },
      u: {
        type: actions.updateBroker.SUCCESS,
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
      v: {
        type: actions.updateBroker.SUCCESS,
        payload: {
          brokerId: bkId,
          entities: {
            brokers: {
              [bkId]: {
                ...brokerEntity,
                nodeNames: ['n1', 'n2'],
              },
            },
          },
          result: bkId,
        },
      },
      w: {
        type: actions.updateBroker.SUCCESS,
        payload: {
          brokerId: bkId,
          entities: {
            brokers: {
              [bkId]: {
                ...brokerEntity,
                clientPort: 1234,
              },
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

it('throw exception of update broker should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock update broker failed',
  };
  const spyCreate = jest
    .spyOn(brokerApi, 'update')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.updateBroker.TRIGGER,
        payload: { values: { ...brokerEntity } },
      },
    });
    const output$ = updateBrokerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateBroker.REQUEST,
        payload: { brokerId: bkId },
      },
      e: {
        type: actions.updateBroker.FAILURE,
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
