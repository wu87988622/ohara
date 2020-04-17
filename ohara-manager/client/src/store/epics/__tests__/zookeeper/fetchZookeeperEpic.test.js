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

import fetchZookeeperEpic from '../../zookeeper/fetchZookeeperEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as zookeeperEntity } from 'api/__mocks__/zookeeperApi';
import { zookeeperInfoEntity } from 'api/__mocks__/inspectApi';

jest.mock('api/zookeeperApi');
jest.mock('api/inspectApi');

const key = { name: 'newzk', group: 'newworkspace' };
const zkId = getId(key);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('fetch zookeeper should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a 10s     ';
    const expected = '--a 2999ms u';
    const subs = '    ^-----------';

    const action$ = hot(input, {
      a: {
        type: actions.fetchZookeeper.TRIGGER,
        payload: key,
      },
    });
    const output$ = fetchZookeeperEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      u: {
        type: actions.fetchZookeeper.SUCCESS,
        payload: {
          zookeeperId: zkId,
          entities: {
            zookeepers: {
              [zkId]: { ...zookeeperEntity, ...key },
            },
            infos: {
              [zkId]: { ...zookeeperInfoEntity, ...key },
            },
          },
          result: zkId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch zookeeper multiple times within period should get first result', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const anotherKey = { name: 'anotherzk', group: 'newworkspace' };
    const input = '   ^-a 50ms b   ';
    const expected = '--a 2999ms u-';
    const subs = '    ^------------';

    const action$ = hot(input, {
      a: {
        type: actions.fetchZookeeper.TRIGGER,
        payload: key,
      },
      b: {
        type: actions.fetchZookeeper.TRIGGER,
        payload: anotherKey,
      },
    });
    const output$ = fetchZookeeperEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      u: {
        type: actions.fetchZookeeper.SUCCESS,
        payload: {
          zookeeperId: zkId,
          entities: {
            zookeepers: {
              [zkId]: { ...zookeeperEntity, ...key },
            },
            infos: {
              [zkId]: { ...zookeeperInfoEntity, ...key },
            },
          },
          result: zkId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch zookeeper multiple times without period should get latest result', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const anotherKey = { name: 'anotherzk', group: 'newworkspace' };
    const input = '   ^-a 2s b         ';
    const expected = '--a 2s b 2999ms u';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.fetchZookeeper.TRIGGER,
        payload: key,
      },
      b: {
        type: actions.fetchZookeeper.TRIGGER,
        payload: anotherKey,
      },
    });
    const output$ = fetchZookeeperEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      b: {
        type: actions.fetchZookeeper.REQUEST,
        payload: {
          zookeeperId: getId(anotherKey),
        },
      },
      u: {
        type: actions.fetchZookeeper.SUCCESS,
        payload: {
          zookeeperId: getId(anotherKey),
          entities: {
            zookeepers: {
              [getId(anotherKey)]: { ...zookeeperEntity, ...anotherKey },
            },
            infos: {
              [getId(anotherKey)]: { ...zookeeperInfoEntity, ...anotherKey },
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
