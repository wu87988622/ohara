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

import startZookeeperEpic from '../../zookeeper/startZookeeperEpic';
import * as zookeeperApi from 'api/zookeeperApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import { entity as zookeeperEntity } from 'api/__mocks__/zookeeperApi';

jest.mock('api/zookeeperApi');

const zkId = getId(zookeeperEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  // ensure the mock data is as expected before each test
  jest.restoreAllMocks();
});

it('start zookeeper should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a              ';
    const expected = '--a 9ms u 499ms v';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.startZookeeper.TRIGGER,
        payload: zookeeperEntity,
      },
    });
    const output$ = startZookeeperEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      u: {
        data: {
          aliveNodes: [],
          lastModified: 0,
          nodeMetrics: {},
        },
        status: 200,
        title: 'mock start zookeeper data',
      },
      v: {
        type: actions.startZookeeper.SUCCESS,
        payload: {
          zookeeperId: zkId,
          entities: {
            zookeepers: {
              [zkId]: {
                ...zookeeperEntity,
                state: SERVICE_STATE.RUNNING,
              },
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

it('start zookeeper failed after reach retry limit', () => {
  // mock a 20 times "failed started" result
  const spyGet = jest.spyOn(zookeeperApi, 'get');
  for (let i = 0; i < 20; i++) {
    spyGet.mockReturnValueOnce(
      of({
        status: 200,
        title: 'retry mock get data',
        data: {},
      }),
    );
  }
  // get result finally
  spyGet.mockReturnValueOnce(
    of({
      status: 200,
      title: 'retry mock get data',
      data: { ...zookeeperEntity, state: SERVICE_STATE.RUNNING },
    }),
  );

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a                ';
    // we failed after retry 11 times (11 * 2000ms = 22s)
    const expected = '--a 9ms u 21999ms v';
    const subs = '    ^------------------';

    const action$ = hot(input, {
      a: {
        type: actions.startZookeeper.TRIGGER,
        payload: zookeeperEntity,
      },
    });
    const output$ = startZookeeperEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      u: {
        data: {
          aliveNodes: [],
          lastModified: 0,
          nodeMetrics: {},
        },
        status: 200,
        title: 'mock start zookeeper data',
      },
      v: {
        type: actions.startZookeeper.FAILURE,
        payload: 'exceed max retry times',
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('start zookeeper multiple times should be worked once', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---a 1s a 10s ';
    const expected = '--a 9ms u 499ms v';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.startZookeeper.TRIGGER,
        payload: zookeeperEntity,
      },
    });
    const output$ = startZookeeperEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      u: {
        data: {
          aliveNodes: [],
          lastModified: 0,
          nodeMetrics: {},
        },
        status: 200,
        title: 'mock start zookeeper data',
      },
      v: {
        type: actions.startZookeeper.SUCCESS,
        payload: {
          zookeeperId: zkId,
          entities: {
            zookeepers: {
              [zkId]: {
                ...zookeeperEntity,
                state: SERVICE_STATE.RUNNING,
              },
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

it('start different zookeeper should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const anotherZookeeperEntity = {
      ...zookeeperEntity,
      name: 'anotherzk',
      group: 'default',
      xms: 1111,
      xmx: 2222,
      clientPort: 3333,
    };
    const input = '   ^-a--b                       ';
    const expected = '--a--b 6ms u 2ms v 496ms y--z';
    const subs = '    ^----------------------------';

    const action$ = hot(input, {
      a: {
        type: actions.startZookeeper.TRIGGER,
        payload: zookeeperEntity,
      },
      b: {
        type: actions.startZookeeper.TRIGGER,
        payload: anotherZookeeperEntity,
      },
    });
    const output$ = startZookeeperEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      b: {
        type: actions.startZookeeper.REQUEST,
        payload: {
          zookeeperId: getId(anotherZookeeperEntity),
        },
      },
      u: {
        data: {
          aliveNodes: [],
          lastModified: 0,
          nodeMetrics: {},
        },
        status: 200,
        title: 'mock start zookeeper data',
      },
      v: {
        data: {
          aliveNodes: [],
          lastModified: 0,
          nodeMetrics: {},
        },
        status: 200,
        title: 'mock start zookeeper data',
      },
      y: {
        type: actions.startZookeeper.SUCCESS,
        payload: {
          zookeeperId: zkId,
          entities: {
            zookeepers: {
              [zkId]: {
                ...zookeeperEntity,
                state: SERVICE_STATE.RUNNING,
              },
            },
          },
          result: zkId,
        },
      },
      z: {
        type: actions.startZookeeper.SUCCESS,
        payload: {
          zookeeperId: getId(anotherZookeeperEntity),
          entities: {
            zookeepers: {
              [getId(anotherZookeeperEntity)]: {
                ...anotherZookeeperEntity,
                state: SERVICE_STATE.RUNNING,
              },
            },
          },
          result: getId(anotherZookeeperEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
