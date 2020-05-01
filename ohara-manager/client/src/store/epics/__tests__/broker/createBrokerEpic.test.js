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
import createBrokerEpic from '../../broker/createBrokerEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as brokerEntity } from 'api/__mocks__/brokerApi';

jest.mock('api/brokerApi');

const bkId = getId(brokerEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('create broker should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a         ';
    const expected = '--a 1999ms u';
    const subs = '    ^-----------';

    const action$ = hot(input, {
      a: {
        type: actions.createBroker.TRIGGER,
        payload: brokerEntity,
      },
    });
    const output$ = createBrokerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createBroker.REQUEST,
        payload: {
          brokerId: bkId,
        },
      },
      u: {
        type: actions.createBroker.SUCCESS,
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

it('create multiple brokers should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-ab          ';
    const expected = '--ab 1998ms uv';
    const subs = '    ^-------------';
    const anotherBrokerEntity = { ...brokerEntity, name: 'bk01' };

    const action$ = hot(input, {
      a: {
        type: actions.createBroker.TRIGGER,
        payload: brokerEntity,
      },
      b: {
        type: actions.createBroker.TRIGGER,
        payload: anotherBrokerEntity,
      },
    });
    const output$ = createBrokerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createBroker.REQUEST,
        payload: {
          brokerId: bkId,
        },
      },
      u: {
        type: actions.createBroker.SUCCESS,
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
      b: {
        type: actions.createBroker.REQUEST,
        payload: {
          brokerId: getId(anotherBrokerEntity),
        },
      },
      v: {
        type: actions.createBroker.SUCCESS,
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

it('create same broker within period should be created once only', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-aa 10s a    ';
    const expected = '--a 1999ms u--';
    const subs = '    ^-------------';

    const action$ = hot(input, {
      a: {
        type: actions.createBroker.TRIGGER,
        payload: brokerEntity,
      },
    });
    const output$ = createBrokerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createBroker.REQUEST,
        payload: {
          brokerId: bkId,
        },
      },
      u: {
        type: actions.createBroker.SUCCESS,
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

it('throw exception of create broker should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock create broker failed',
  };
  const spyCreate = jest
    .spyOn(brokerApi, 'create')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.createBroker.TRIGGER,
        payload: brokerEntity,
      },
    });
    const output$ = createBrokerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createBroker.REQUEST,
        payload: { brokerId: bkId },
      },
      e: {
        type: actions.createBroker.FAILURE,
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
