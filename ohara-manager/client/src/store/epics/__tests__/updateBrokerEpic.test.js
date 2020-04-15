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

import updateBrokerEpic from '../broker/updateBrokerEpic';
import { entity as brokerEntity } from 'api/__mocks__/brokerApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';

jest.mock('api/brokerApi');

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('update broker should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.updateBroker.TRIGGER,
        payload: { ...brokerEntity, jmxPort: 999 },
      },
    });
    const output$ = updateBrokerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateBroker.REQUEST,
        payload: {
          brokerId: getId(brokerEntity),
        },
      },
      u: {
        type: actions.updateBroker.SUCCESS,
        payload: {
          brokerId: getId(brokerEntity),
          entities: {
            brokers: {
              [getId(brokerEntity)]: { ...brokerEntity, jmxPort: 999 },
            },
          },
          result: getId(brokerEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('update broker multiple times should got latest result', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 60ms c 10s            ';
    const expected = '--a-b 60ms d 36ms u-v 60ms w';
    const subs = '    ^---------------------------';

    const action$ = hot(input, {
      a: {
        type: actions.updateBroker.TRIGGER,
        payload: brokerEntity,
      },
      b: {
        type: actions.updateBroker.TRIGGER,
        payload: { ...brokerEntity, nodeNames: ['n1', 'n2'] },
      },
      c: {
        type: actions.updateBroker.TRIGGER,
        payload: { ...brokerEntity, clientPort: 1234 },
      },
    });
    const output$ = updateBrokerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateBroker.REQUEST,
        payload: {
          brokerId: getId(brokerEntity),
        },
      },
      b: {
        type: actions.updateBroker.REQUEST,
        payload: {
          brokerId: getId(brokerEntity),
        },
      },
      d: {
        type: actions.updateBroker.REQUEST,
        payload: {
          brokerId: getId(brokerEntity),
        },
      },
      u: {
        type: actions.updateBroker.SUCCESS,
        payload: {
          brokerId: getId(brokerEntity),
          entities: {
            brokers: {
              [getId(brokerEntity)]: brokerEntity,
            },
          },
          result: getId(brokerEntity),
        },
      },
      v: {
        type: actions.updateBroker.SUCCESS,
        payload: {
          brokerId: getId(brokerEntity),
          entities: {
            brokers: {
              [getId(brokerEntity)]: {
                ...brokerEntity,
                nodeNames: ['n1', 'n2'],
              },
            },
          },
          result: getId(brokerEntity),
        },
      },
      w: {
        type: actions.updateBroker.SUCCESS,
        payload: {
          brokerId: getId(brokerEntity),
          entities: {
            brokers: {
              [getId(brokerEntity)]: {
                ...brokerEntity,
                clientPort: 1234,
              },
            },
          },
          result: getId(brokerEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
