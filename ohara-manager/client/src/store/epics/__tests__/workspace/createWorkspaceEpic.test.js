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

import createWorkspaceEpic from '../../workspace/createWorkspaceEpic';
import * as actions from 'store/actions';
import { entity as zookeeperEntity } from 'api/__mocks__/zookeeperApi';
import { entity as brokerEntity } from 'api/__mocks__/brokerApi';
import { entity as workerEntity } from 'api/__mocks__/workerApi';
import { entity as workspaceEntity } from 'api/__mocks__/workspaceApi';
import { StateObservable } from 'redux-observable';
import { getId, getKey } from 'utils/object';
import { ENTITY_TYPE } from 'store/schema';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import { FORM, LOG_LEVEL } from 'const';

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

it('create workspace should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a            ';
    const expected = `--a 2999ms b\
                      999ms c 1999ms d\
                      999ms e 1999ms f\
                      999ms g 1999ms h\
                      999ms m  499ms n\
                      999ms o  499ms p\
                      999ms q  499ms r\
                      999ms (tuvwxyz)`;
    const subs = '    ^--------------';

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
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
