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
import * as zookeeperApi from 'api/zookeeperApi';
import deleteZookeeperEpic from '../../zookeeper/deleteZookeeperEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as zookeeperEntity } from 'api/__mocks__/zookeeperApi';

jest.mock('api/zookeeperApi');

const zkId = getId(zookeeperEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('delete zookeeper should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 99ms u';
    const subs = ['   ^----------', '--^ 99ms !'];

    const action$ = hot(input, {
      a: {
        type: actions.deleteZookeeper.TRIGGER,
        payload: { values: zookeeperEntity },
      },
    });
    const output$ = deleteZookeeperEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      u: {
        type: actions.deleteZookeeper.SUCCESS,
        payload: {
          zookeeperId: zkId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete multiple zookeepers should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-ab         ';
    const expected = '--ab 98ms uv';
    const subs = ['   ^------------', '--^ 99ms !', '---^ 99ms !'];
    const anotherZookeeperEntity = { ...zookeeperEntity, name: 'zk01' };

    const action$ = hot(input, {
      a: {
        type: actions.deleteZookeeper.TRIGGER,
        payload: { values: zookeeperEntity },
      },
      b: {
        type: actions.deleteZookeeper.TRIGGER,
        payload: { values: anotherZookeeperEntity },
      },
    });
    const output$ = deleteZookeeperEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      u: {
        type: actions.deleteZookeeper.SUCCESS,
        payload: {
          zookeeperId: zkId,
        },
      },
      b: {
        type: actions.deleteZookeeper.REQUEST,
        payload: {
          zookeeperId: getId(anotherZookeeperEntity),
        },
      },
      v: {
        type: actions.deleteZookeeper.SUCCESS,
        payload: {
          zookeeperId: getId(anotherZookeeperEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete same zookeeper within period should be created once only', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-aa 10s a---';
    const expected = '--a 99ms u--';
    const subs = ['   ^------------', '--^ 99ms !'];

    const action$ = hot(input, {
      a: {
        type: actions.deleteZookeeper.TRIGGER,
        payload: { values: zookeeperEntity },
      },
    });
    const output$ = deleteZookeeperEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      u: {
        type: actions.deleteZookeeper.SUCCESS,
        payload: {
          zookeeperId: zkId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of delete zookeeper should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock delete zookeeper failed',
  };
  const spyDelete = jest
    .spyOn(zookeeperApi, 'remove')
    .mockReturnValue(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = ['   ^-------!', '--(^!)'];

    const action$ = hot(input, {
      a: {
        type: actions.deleteZookeeper.TRIGGER,
        payload: { values: zookeeperEntity },
      },
    });
    const output$ = deleteZookeeperEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteZookeeper.REQUEST,
        payload: { zookeeperId: zkId },
      },
      e: {
        type: actions.deleteZookeeper.FAILURE,
        payload: { ...error, zookeeperId: zkId },
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

    expect(spyDelete).toHaveBeenCalled();
  });
});
