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

jest.mock('api/workspaceApi');
jest.mock('api/workerApi');
jest.mock('api/brokerApi');
jest.mock('api/zookeeperApi');

const workspaceId = getId(workspaceEntity);
const workspaceKey = getKey(workspaceEntity);
const workerId = getId(workerEntity);
const workerKey = getKey(workerEntity);
const brokerId = getId(brokerEntity);
const brokerKey = getKey(brokerEntity);
const zookeeperId = getId(zookeeperEntity);
const zookeeperKey = getKey(zookeeperEntity);

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

//TODO : re enable this in #4898
it.skip('delete workspace should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a                ';
    const stateInput = 'v';
    const expected = `--a 499ms b 199ms z`;
    const subs = ['^-', '--^ 699ms !'];

    const state$ = new StateObservable(
      hot(stateInput, {
        v: stateValue,
      }),
    );
    const action$ = hot(input, {
      a: {
        type: actions.deleteWorkspace.TRIGGER,
        payload: {
          zookeeperKey,
          brokerKey,
          workerKey,
          workspaceKey,
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
