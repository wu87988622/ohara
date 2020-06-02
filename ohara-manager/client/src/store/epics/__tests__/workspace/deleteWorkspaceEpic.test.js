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

import { omit } from 'lodash';
import { TestScheduler } from 'rxjs/testing';
import { StateObservable } from 'redux-observable';

import deleteWorkspaceEpic from '../../workspace/deleteWorkspaceEpic';
import * as actions from 'store/actions';
import { ENTITY_TYPE } from 'store/schema';
import { getId, getKey } from 'utils/object';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import { entity as workspaceEntity } from 'api/__mocks__/workspaceApi';
import { entity as workerEntity } from 'api/__mocks__/workerApi';
import { entity as brokerEntity } from 'api/__mocks__/brokerApi';
import { entity as zookeeperEntity } from 'api/__mocks__/zookeeperApi';
import { entity as connectorEntity } from 'api/__mocks__/connectorApi';
import { entity as streamEntity } from 'api/__mocks__/streamApi';
import { entity as shabondiEntity } from 'api/__mocks__/shabondiApi';
import { entity as pipelineEntity } from 'api/__mocks__/pipelineApi';

jest.mock('api/workspaceApi');
jest.mock('api/workerApi');
jest.mock('api/brokerApi');
jest.mock('api/zookeeperApi');
jest.mock('api/connectorApi');
jest.mock('api/streamApi');
jest.mock('api/shabondiApi');
jest.mock('api/pipelineApi');

const workspaceId = getId(workspaceEntity);
const workspaceKey = getKey(workspaceEntity);
const workerId = getId(workerEntity);
const workerKey = getKey(workerEntity);
const brokerId = getId(brokerEntity);
const brokerKey = getKey(brokerEntity);
const zookeeperId = getId(zookeeperEntity);
const zookeeperKey = getKey(zookeeperEntity);
const connecterKey = getKey(connectorEntity);
const connectorId = getId(connectorEntity);
const streamKey = getKey(streamEntity);
const streamId = getId(streamEntity);
const shabondiKey = getKey(shabondiEntity);
const shabondiId = getId(shabondiEntity);
const pipelineKey = getKey(pipelineEntity);
const pipelineId = getId(pipelineEntity);

const stateValue = {
  entities: {
    [ENTITY_TYPE.workspaces]: {
      [workspaceId]: workspaceEntity,
    },
    [ENTITY_TYPE.workers]: {
      [workerId]: { ...workerEntity, state: SERVICE_STATE.RUNNING },
    },
    [ENTITY_TYPE.brokers]: {
      [brokerId]: { ...brokerEntity, state: SERVICE_STATE.RUNNING },
    },
    [ENTITY_TYPE.zookeepers]: {
      [zookeeperId]: { ...zookeeperEntity, state: SERVICE_STATE.RUNNING },
    },
  },
};

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  // ensure the mock data is as expected before each test
  jest.restoreAllMocks();
});

it('delete workspace when worker is running should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '---^-a           ';
    const stateInput = '--v';
    // Expect stop worker
    const expected = `--a 499ms (bz)`;
    const subs = ['^-', '--^ 499ms !'];

    const state$ = new StateObservable(
      hot(stateInput, {
        v: stateValue,
      }),
    );
    const action$ = hot(input, {
      a: {
        type: actions.deleteWorkspace.TRIGGER,
        payload: {
          values: {
            zookeeperKey,
            brokerKey,
            workerKey,
            workspaceKey,
            pipelineKeys: [],
            connectorKeys: [],
            shabondiKeys: [],
            streamKeys: [],
            files: [],
          },
        },
      },
    });
    const output$ = deleteWorkspaceEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopWorker.REQUEST,
        payload: { workerId },
      },
      b: {
        type: actions.stopWorker.SUCCESS,
        payload: {
          entities: {
            workers: {
              [workerId]: omit(workerEntity, 'state'),
            },
          },
          result: workerId,
          workerId,
        },
      },
      z: {
        type: actions.initializeApp.TRIGGER,
        payload: {},
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete workspace and some components when worker is running should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '---^-a           ';
    const stateInput = '--v';
    // Expect delete all components first, then stop worker
    const expected = `--a 999ms (bcd) 995ms (efg) 995ms (hij) 495ms (klm) 495ms (nz)`;
    const subs = ['^-', '--^ 3999ms !'];

    const state$ = new StateObservable(
      hot(stateInput, {
        v: stateValue,
      }),
    );
    const action$ = hot(input, {
      a: {
        type: actions.deleteWorkspace.TRIGGER,
        payload: {
          values: {
            zookeeperKey,
            brokerKey,
            workerKey,
            workspaceKey,
            pipelineKeys: [pipelineKey],
            connectorKeys: [connecterKey],
            shabondiKeys: [shabondiKey],
            streamKeys: [streamKey],
            files: [],
          },
        },
      },
    });
    const output$ = deleteWorkspaceEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteConnector.REQUEST,
        payload: { connectorId },
      },
      b: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      c: {
        type: actions.deleteConnector.SUCCESS,
        payload: { connectorId },
      },
      d: {
        type: actions.deleteStream.REQUEST,
        payload: { streamId },
      },
      e: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      f: {
        type: actions.deleteStream.SUCCESS,
        payload: { streamId },
      },
      g: {
        type: actions.deleteShabondi.REQUEST,
        payload: { shabondiId },
      },
      h: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      i: {
        type: actions.deleteShabondi.SUCCESS,
        payload: { shabondiId },
      },
      j: {
        type: actions.deletePipeline.REQUEST,
        payload: { pipelineId },
      },
      k: {
        type: actions.deletePipeline.SUCCESS,
        payload: { pipelineId },
      },
      l: {
        type: actions.switchPipeline.TRIGGER,
      },
      m: {
        type: actions.stopWorker.REQUEST,
        payload: { workerId },
      },
      n: {
        type: actions.stopWorker.SUCCESS,
        payload: {
          entities: {
            workers: {
              [workerId]: omit(workerEntity, 'state'),
            },
          },
          result: workerId,
          workerId,
        },
      },
      z: {
        type: actions.initializeApp.TRIGGER,
        payload: {},
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete workspace when broker is running should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '---^-a           ';
    const stateInput = '--v';
    // Expect stop broker and remove object of worker
    const expected = `--a 499ms (bm) 996ms (nz)`;
    const subs = ['^-', '--^ 1499ms !'];

    const state$ = new StateObservable(
      hot(stateInput, {
        v: omit(
          stateValue,
          `entities[${[ENTITY_TYPE.workers]}][${workerId}].state`,
        ),
      }),
    );
    const action$ = hot(input, {
      a: {
        type: actions.deleteWorkspace.TRIGGER,
        payload: {
          values: {
            zookeeperKey,
            brokerKey,
            workerKey,
            workspaceKey,
            pipelineKeys: [],
            connectorKeys: [],
            shabondiKeys: [],
            streamKeys: [],
            files: [],
          },
        },
      },
    });
    const output$ = deleteWorkspaceEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopBroker.REQUEST,
        payload: { brokerId },
      },
      b: {
        type: actions.stopBroker.SUCCESS,
        payload: {
          entities: {
            brokers: {
              [brokerId]: omit(brokerEntity, 'state'),
            },
          },
          result: brokerId,
          brokerId,
        },
      },
      m: {
        type: actions.deleteWorker.REQUEST,
        payload: { workerId },
      },
      n: {
        type: actions.deleteWorker.SUCCESS,
        payload: { workerId },
      },
      z: {
        type: actions.initializeApp.TRIGGER,
        payload: {},
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete workspace when zookeeper is running should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '---^-a           ';
    const stateInput = '--v';
    // Expect stop zookeeper and remove object of worker and broker
    const expected = `--a 499ms (bm) 996ms (no) 996ms (pz)`;
    const subs = ['^-', '--^ 2499ms !'];

    const state$ = new StateObservable(
      hot(stateInput, {
        v: omit(stateValue, [
          `entities[${[ENTITY_TYPE.workers]}][${workerId}].state`,
          `entities[${[ENTITY_TYPE.brokers]}][${brokerId}].state`,
        ]),
      }),
    );
    const action$ = hot(input, {
      a: {
        type: actions.deleteWorkspace.TRIGGER,
        payload: {
          values: {
            zookeeperKey,
            brokerKey,
            workerKey,
            workspaceKey,
            pipelineKeys: [],
            connectorKeys: [],
            shabondiKeys: [],
            streamKeys: [],
            files: [],
          },
        },
      },
    });
    const output$ = deleteWorkspaceEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopZookeeper.REQUEST,
        payload: { zookeeperId },
      },
      b: {
        type: actions.stopZookeeper.SUCCESS,
        payload: {
          entities: {
            zookeepers: {
              [zookeeperId]: omit(zookeeperEntity, 'state'),
            },
          },
          result: zookeeperId,
          zookeeperId,
        },
      },
      m: {
        type: actions.deleteWorker.REQUEST,
        payload: { workerId },
      },
      n: {
        type: actions.deleteWorker.SUCCESS,
        payload: { workerId },
      },
      o: {
        type: actions.deleteBroker.REQUEST,
        payload: { brokerId },
      },
      p: {
        type: actions.deleteBroker.SUCCESS,
        payload: { brokerId },
      },
      z: {
        type: actions.initializeApp.TRIGGER,
        payload: {},
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete workspace when all services are stopped should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '---^-a           ';
    const stateInput = '--v';
    // Expect remove object of service only
    const expected = `--m 999ms (no) 996ms (pq) 996ms (rz)`;
    const subs = ['^-', '--^ 2999ms !'];

    const state$ = new StateObservable(
      hot(stateInput, {
        v: omit(stateValue, [
          `entities[${[ENTITY_TYPE.workers]}][${workerId}].state`,
          `entities[${[ENTITY_TYPE.brokers]}][${brokerId}].state`,
          `entities[${[ENTITY_TYPE.zookeepers]}][${zookeeperId}].state`,
        ]),
      }),
    );
    const action$ = hot(input, {
      a: {
        type: actions.deleteWorkspace.TRIGGER,
        payload: {
          values: {
            zookeeperKey,
            brokerKey,
            workerKey,
            workspaceKey,
            pipelineKeys: [],
            connectorKeys: [],
            shabondiKeys: [],
            streamKeys: [],
            files: [],
          },
        },
      },
    });
    const output$ = deleteWorkspaceEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      m: {
        type: actions.deleteWorker.REQUEST,
        payload: { workerId },
      },
      n: {
        type: actions.deleteWorker.SUCCESS,
        payload: { workerId },
      },
      o: {
        type: actions.deleteBroker.REQUEST,
        payload: { brokerId },
      },
      p: {
        type: actions.deleteBroker.SUCCESS,
        payload: { brokerId },
      },
      q: {
        type: actions.deleteZookeeper.REQUEST,
        payload: { zookeeperId },
      },
      r: {
        type: actions.deleteZookeeper.SUCCESS,
        payload: { zookeeperId },
      },
      z: {
        type: actions.initializeApp.TRIGGER,
        payload: {},
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
