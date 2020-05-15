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

import { of } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';
import { StateObservable } from 'redux-observable';

import { FORM, LOG_LEVEL, GROUP } from 'const';
import * as brokerApi from 'api/brokerApi';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import * as actions from 'store/actions';
import { ENTITY_TYPE } from 'store/schema';
import { getId, getKey } from 'utils/object';
import createWorkspaceEpic from '../../workspace/createWorkspaceEpic';
import { entity as zookeeperEntity } from 'api/__mocks__/zookeeperApi';
import { entity as brokerEntity } from 'api/__mocks__/brokerApi';
import { entity as workerEntity } from 'api/__mocks__/workerApi';
import { entity as workspaceEntity } from 'api/__mocks__/workspaceApi';

jest.mock('api/zookeeperApi');
jest.mock('api/brokerApi');
jest.mock('api/workerApi');
jest.mock('api/workspaceApi');

const zkId = getId(zookeeperEntity);
const bkId = getId(brokerEntity);
const wkId = getId(workerEntity);
const workspaceId = getId(workspaceEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  // ensure the mock data is as expected before each test
  jest.restoreAllMocks();
});

it('create workspace should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a                ';
    const expected = `--a 2999ms b       \
                      999ms c 1999ms (id)\
                      996ms e 1999ms (jf)\
                      996ms g 1999ms (kh)\
                      996ms m  499ms n   \
                      999ms o  499ms p   \
                      999ms q  499ms r   \
                      999ms (tuvwxyzl)`;
    const subs = '    ^------------------';

    const action$ = hot(input, {
      a: {
        type: actions.createWorkspace.TRIGGER,
        payload: {
          workspace: workspaceEntity,
          zookeeper: zookeeperEntity,
          broker: brokerEntity,
          worker: workerEntity,
        },
      },
    });
    const state$ = new StateObservable(hot('-'));
    const output$ = createWorkspaceEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createWorkspace.REQUEST,
      },
      b: {
        type: actions.createWorkspace.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.workspaces]: {
              [workspaceId]: workspaceEntity,
            },
          },
          result: workspaceId,
        },
      },
      c: {
        type: actions.createZookeeper.REQUEST,
        payload: { zookeeperId: zkId },
      },
      i: {
        type: actions.updateWorkspace.TRIGGER,
        payload: {
          name: zookeeperEntity.name,
          group: GROUP.WORKSPACE,
          zookeeper: zookeeperEntity,
        },
      },
      d: {
        type: actions.createZookeeper.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.zookeepers]: {
              [zkId]: zookeeperEntity,
            },
          },
          zookeeperId: zkId,
          result: zkId,
        },
      },
      e: {
        type: actions.createBroker.REQUEST,
        payload: { brokerId: bkId },
      },
      j: {
        type: actions.updateWorkspace.TRIGGER,
        payload: {
          name: brokerEntity.name,
          group: GROUP.WORKSPACE,
          broker: brokerEntity,
        },
      },
      f: {
        type: actions.createBroker.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.brokers]: {
              [bkId]: brokerEntity,
            },
          },
          brokerId: bkId,
          result: bkId,
        },
      },
      g: {
        type: actions.createWorker.REQUEST,
        payload: { workerId: wkId },
      },
      k: {
        type: actions.updateWorkspace.TRIGGER,
        payload: {
          name: workerEntity.name,
          group: GROUP.WORKSPACE,
          worker: workerEntity,
        },
      },
      h: {
        type: actions.createWorker.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.workers]: {
              [wkId]: workerEntity,
            },
          },
          workerId: wkId,
          result: wkId,
        },
      },
      m: {
        type: actions.startZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      n: {
        type: actions.startZookeeper.SUCCESS,
        payload: {
          zookeeperId: zkId,
          entities: {
            [ENTITY_TYPE.zookeepers]: {
              [zkId]: {
                ...zookeeperEntity,
                state: SERVICE_STATE.RUNNING,
              },
            },
          },
          result: zkId,
        },
      },
      o: {
        type: actions.startBroker.REQUEST,
        payload: {
          brokerId: bkId,
        },
      },
      p: {
        type: actions.startBroker.SUCCESS,
        payload: {
          brokerId: bkId,
          entities: {
            [ENTITY_TYPE.brokers]: {
              [bkId]: {
                ...brokerEntity,
                state: SERVICE_STATE.RUNNING,
              },
            },
          },
          result: bkId,
        },
      },
      q: {
        type: actions.startWorker.REQUEST,
        payload: {
          workerId: wkId,
        },
      },
      r: {
        type: actions.startWorker.SUCCESS,
        payload: {
          workerId: wkId,
          entities: {
            [ENTITY_TYPE.workers]: {
              [wkId]: {
                ...workerEntity,
                state: SERVICE_STATE.RUNNING,
              },
            },
          },
          result: wkId,
        },
      },
      t: {
        type: actions.createWorkspace.FULFILL,
      },
      u: {
        type: '@@redux-form/RESET',
        meta: { form: FORM.CREATE_WORKSPACE },
      },
      v: {
        type: actions.switchCreateWorkspaceStep.TRIGGER,
        payload: 0,
      },
      w: {
        type: actions.closeCreateWorkspace.TRIGGER,
      },
      x: {
        type: actions.closeIntro.TRIGGER,
      },
      y: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          title: 'Successfully created workspace workspace1.',
          type: LOG_LEVEL.info,
        },
      },
      z: {
        type: actions.switchWorkspace.TRIGGER,
        payload: getKey(workspaceEntity),
      },
      l: {
        type: actions.fetchNodes.TRIGGER,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('create workspace should be failure if one of the services start failed', () => {
  const spyGet = jest.spyOn(brokerApi, 'get');
  spyGet.mockReturnValue(
    of({
      status: 200,
      title: 'retry mock get data',
      data: {},
    }),
  );

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a               ';
    const expected = `--a 2999ms b       \
                      999ms c 1999ms (id)\
                      996ms e 1999ms (jf)\
                      996ms g 1999ms (kh)\
                      996ms m  499ms n   \
                      999ms o 20999ms (xy)`;
    const subs = '    ^------------------';

    const action$ = hot(input, {
      a: {
        type: actions.createWorkspace.TRIGGER,
        payload: {
          workspace: workspaceEntity,
          zookeeper: zookeeperEntity,
          broker: brokerEntity,
          worker: workerEntity,
        },
      },
    });
    const state$ = new StateObservable(hot('-'));
    const output$ = createWorkspaceEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createWorkspace.REQUEST,
      },
      b: {
        type: actions.createWorkspace.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.workspaces]: {
              [workspaceId]: workspaceEntity,
            },
          },
          result: workspaceId,
        },
      },
      c: {
        type: actions.createZookeeper.REQUEST,
        payload: { zookeeperId: zkId },
      },
      i: {
        type: actions.updateWorkspace.TRIGGER,
        payload: {
          name: zookeeperEntity.name,
          group: GROUP.WORKSPACE,
          zookeeper: zookeeperEntity,
        },
      },
      d: {
        type: actions.createZookeeper.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.zookeepers]: {
              [zkId]: zookeeperEntity,
            },
          },
          zookeeperId: zkId,
          result: zkId,
        },
      },
      e: {
        type: actions.createBroker.REQUEST,
        payload: { brokerId: bkId },
      },
      j: {
        type: actions.updateWorkspace.TRIGGER,
        payload: {
          name: brokerEntity.name,
          group: GROUP.WORKSPACE,
          broker: brokerEntity,
        },
      },
      f: {
        type: actions.createBroker.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.brokers]: {
              [bkId]: brokerEntity,
            },
          },
          brokerId: bkId,
          result: bkId,
        },
      },
      g: {
        type: actions.createWorker.REQUEST,
        payload: { workerId: wkId },
      },
      k: {
        type: actions.updateWorkspace.TRIGGER,
        payload: {
          name: workerEntity.name,
          group: GROUP.WORKSPACE,
          worker: workerEntity,
        },
      },
      h: {
        type: actions.createWorker.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.workers]: {
              [wkId]: workerEntity,
            },
          },
          workerId: wkId,
          result: wkId,
        },
      },
      m: {
        type: actions.startZookeeper.REQUEST,
        payload: {
          zookeeperId: zkId,
        },
      },
      n: {
        type: actions.startZookeeper.SUCCESS,
        payload: {
          zookeeperId: zkId,
          entities: {
            [ENTITY_TYPE.zookeepers]: {
              [zkId]: {
                ...zookeeperEntity,
                state: SERVICE_STATE.RUNNING,
              },
            },
          },
          result: zkId,
        },
      },
      o: {
        type: actions.startBroker.REQUEST,
        payload: {
          brokerId: bkId,
        },
      },
      x: {
        type: actions.createWorkspace.FAILURE,
        payload: {
          title: 'start broker exceeded max retry count',
        },
      },
      y: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          title: 'start broker exceeded max retry count',
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
