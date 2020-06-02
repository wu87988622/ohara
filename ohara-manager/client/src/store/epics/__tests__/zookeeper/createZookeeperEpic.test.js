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

import { LOG_LEVEL, GROUP } from 'const';
import * as zookeeperApi from 'api/zookeeperApi';
import createZookeeperEpic from '../../zookeeper/createZookeeperEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as zookeeperEntity } from 'api/__mocks__/zookeeperApi';

jest.mock('api/zookeeperApi');

const zkId = getId(zookeeperEntity);
const workspaceKey = { name: zookeeperEntity.name, group: GROUP.WORKSPACE };

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('create zookeeper should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a            ';
    const expected = '--a 1999ms (bu)';
    const subs = '    ^--------------';

    const action$ = hot(input, {
      a: {
        type: actions.createZookeeper.TRIGGER,
        payload: zookeeperEntity,
      },
    });
    const output$ = createZookeeperEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      b: {
        type: actions.updateWorkspace.TRIGGER,
        payload: {
          ...workspaceKey,
          zookeeper: zookeeperEntity,
        },
      },
      u: {
        type: actions.createZookeeper.SUCCESS,
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
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('create multiple zookeepers should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---b                ';
    const expected = '--a---b 1995ms (xu)(yv)';
    const subs = '    ^----------------------';
    const anotherZookeeperEntity = { ...zookeeperEntity, name: 'zk01' };

    const action$ = hot(input, {
      a: {
        type: actions.createZookeeper.TRIGGER,
        payload: zookeeperEntity,
      },
      b: {
        type: actions.createZookeeper.TRIGGER,
        payload: anotherZookeeperEntity,
      },
    });
    const output$ = createZookeeperEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      x: {
        type: actions.updateWorkspace.TRIGGER,
        payload: {
          ...workspaceKey,
          zookeeper: zookeeperEntity,
        },
      },
      u: {
        type: actions.createZookeeper.SUCCESS,
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
      b: {
        type: actions.createZookeeper.REQUEST,
        payload: {
          zookeeperId: getId(anotherZookeeperEntity),
        },
      },
      y: {
        type: actions.updateWorkspace.TRIGGER,
        payload: {
          name: anotherZookeeperEntity.name,
          group: GROUP.WORKSPACE,
          zookeeper: anotherZookeeperEntity,
        },
      },
      v: {
        type: actions.createZookeeper.SUCCESS,
        payload: {
          zookeeperId: getId(anotherZookeeperEntity),
          entities: {
            zookeepers: {
              [getId(anotherZookeeperEntity)]: anotherZookeeperEntity,
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

it('create same zookeeper within period should be created once only', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-aa 10s a       ';
    const expected = '--a 1999ms (bu)--';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.createZookeeper.TRIGGER,
        payload: zookeeperEntity,
      },
    });
    const output$ = createZookeeperEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      b: {
        type: actions.updateWorkspace.TRIGGER,
        payload: {
          ...workspaceKey,
          zookeeper: zookeeperEntity,
        },
      },
      u: {
        type: actions.createZookeeper.SUCCESS,
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
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of create zookeeper should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock create zookeeper failed',
  };
  const spyCreate = jest
    .spyOn(zookeeperApi, 'create')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.createZookeeper.TRIGGER,
        payload: zookeeperEntity,
      },
    });
    const output$ = createZookeeperEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createZookeeper.REQUEST,
        payload: { zookeeperId: zkId },
      },
      e: {
        type: actions.createZookeeper.FAILURE,
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
