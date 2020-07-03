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
import updateZookeeperEpic from '../../zookeeper/updateZookeeperEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as zookeeperEntity } from 'api/__mocks__/zookeeperApi';

jest.mock('api/zookeeperApi');

const zkId = getId(zookeeperEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('update zookeeper should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.updateZookeeper.TRIGGER,
        payload: { values: { ...zookeeperEntity, jmxPort: 999 } },
      },
    });
    const output$ = updateZookeeperEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      u: {
        type: actions.updateZookeeper.SUCCESS,
        payload: {
          zookeeperId: zkId,
          entities: {
            zookeepers: {
              [zkId]: { ...zookeeperEntity, jmxPort: 999 },
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

it('update zookeeper multiple times should got latest result', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 60ms c 10s            ';
    const expected = '--a-b 60ms d 36ms u-v 60ms w';
    const subs = '    ^---------------------------';

    const action$ = hot(input, {
      a: {
        type: actions.updateZookeeper.TRIGGER,
        payload: { values: { ...zookeeperEntity } },
      },
      b: {
        type: actions.updateZookeeper.TRIGGER,
        payload: { values: { ...zookeeperEntity, nodeNames: ['n1', 'n2'] } },
      },
      c: {
        type: actions.updateZookeeper.TRIGGER,
        payload: { values: { ...zookeeperEntity, clientPort: 1234 } },
      },
    });
    const output$ = updateZookeeperEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      b: {
        type: actions.updateZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      d: {
        type: actions.updateZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      u: {
        type: actions.updateZookeeper.SUCCESS,
        payload: {
          zookeeperId: zkId,
          entities: {
            zookeepers: {
              [zkId]: zookeeperEntity,
            },
          },
          result: zkId,
        },
      },
      v: {
        type: actions.updateZookeeper.SUCCESS,
        payload: {
          zookeeperId: zkId,
          entities: {
            zookeepers: {
              [zkId]: {
                ...zookeeperEntity,
                nodeNames: ['n1', 'n2'],
              },
            },
          },
          result: zkId,
        },
      },
      w: {
        type: actions.updateZookeeper.SUCCESS,
        payload: {
          zookeeperId: zkId,
          entities: {
            zookeepers: {
              [zkId]: {
                ...zookeeperEntity,
                clientPort: 1234,
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

it('throw exception of update zookeeper should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock update zookeeper failed',
  };
  const spyCreate = jest
    .spyOn(zookeeperApi, 'update')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.updateZookeeper.TRIGGER,
        payload: { values: { ...zookeeperEntity } },
      },
    });
    const output$ = updateZookeeperEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateZookeeper.REQUEST,
        payload: { zookeeperId: zkId },
      },
      e: {
        type: actions.updateZookeeper.FAILURE,
        payload: { ...error, zookeeperId: zkId },
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          ...error,
          zookeeperId: zkId,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyCreate).toHaveBeenCalled();
  });
});
