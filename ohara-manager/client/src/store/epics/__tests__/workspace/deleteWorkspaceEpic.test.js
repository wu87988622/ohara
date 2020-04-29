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
import { StateObservable } from 'redux-observable';

import { LOG_LEVEL, ACTIONS } from 'const';
import deleteWorkspaceEpic from '../../workspace/deleteWorkspaceEpic';
import * as actions from 'store/actions';
import { getKey, getId } from 'utils/object';
import { entity as workspaceEntity } from 'api/__mocks__/workspaceApi';
import { entity as workerEntity } from 'api/__mocks__/workerApi';
import { entity as brokerEntity } from 'api/__mocks__/brokerApi';
import { entity as zookeeperEntity } from 'api/__mocks__/zookeeperApi';

jest.mock('api/workspaceApi');
jest.mock('api/workerApi');
jest.mock('api/brokerApi');
jest.mock('api/zookeeperApi');

const workspaceKey = getKey(workspaceEntity);
const wkKey = getKey(workerEntity);
const bkKey = getKey(brokerEntity);
const zkKey = getKey(zookeeperEntity);
const value = {
  workspace: workspaceKey,
  worker: wkKey,
  broker: bkKey,
  zookeeper: zkKey,
  tmpZookeeper: zookeeperEntity,
  tmpBroker: brokerEntity,
  tmpWorker: workerEntity,
};

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('delete workspace should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a                  ';
    const expected = `--- 10ms 999ms a 499ms b\
                          10ms 999ms c 499ms d\
                          10ms 999ms e 499ms (fm)\
                          997ms 999ms (np)\
                          997ms 999ms (qr)\
                          997ms 999ms (st)\
                          997ms 999ms (uyz)`;
    const subs = ['^-', '--^ 12529ms !'];

    const state$ = new StateObservable(hot('-'));
    const action$ = hot(input, {
      a: {
        type: actions.deleteWorkspace.TRIGGER,
        payload: value,
      },
    });
    const output$ = deleteWorkspaceEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopWorker.REQUEST,
      },
      b: {
        type: actions.stopWorker.SUCCESS,
        payload: {
          ...workerEntity,
          workspaceKey,
        },
      },
      c: {
        type: actions.stopBroker.REQUEST,
      },
      d: {
        type: actions.stopBroker.SUCCESS,
        payload: {
          ...brokerEntity,
          workspaceKey,
        },
      },
      e: {
        type: actions.stopZookeeper.REQUEST,
      },
      f: {
        type: actions.stopZookeeper.SUCCESS,
        payload: {
          ...zookeeperEntity,
          workspaceKey,
        },
      },
      m: {
        type: actions.deleteWorker.REQUEST,
      },
      n: {
        type: actions.deleteWorker.SUCCESS,
        payload: {},
      },
      p: {
        type: actions.deleteBroker.REQUEST,
      },
      q: {
        type: actions.deleteBroker.SUCCESS,
        payload: {},
      },
      r: {
        type: actions.deleteZookeeper.REQUEST,
      },
      s: {
        type: actions.deleteZookeeper.SUCCESS,
        payload: {},
      },
      t: {
        type: actions.deleteWorkspace.REQUEST,
      },
      u: {
        type: actions.deleteWorkspace.SUCCESS,
        payload: getId(workspaceKey),
      },
      y: {
        type: actions.switchCreateWorkspaceStep.TRIGGER,
        payload: 0,
      },
      z: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          title: `Successfully Delete workspace ${workspaceKey.name}.`,
          type: LOG_LEVEL.info,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete workspace in rollback phase include skipList should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const skipList = [
      ACTIONS.DELETE_ZOOKEEPER,
      ACTIONS.DELETE_BROKER,
      ACTIONS.DELETE_WORKER,
      ACTIONS.STOP_WORKER,
      ACTIONS.STOP_BROKER,
      ACTIONS.STOP_ZOOKEEPER,
    ];

    const input = '   ^-a                      ';
    const expected = `--a    999ms 2000ms\
                        b    999ms 10ms m 499ms\
                        (nc) 996ms 2000ms\
                        d    999ms 10ms p 499ms\
                        (qe) 996ms 2000ms\
                        f    999ms 10ms r 499ms\
                        s 74999ms\
                        (yz)`;
    const subs = ['^-', '--^ 88529ms !'];

    const state$ = new StateObservable(hot('-'));
    const action$ = hot(input, {
      a: {
        type: actions.deleteWorkspace.TRIGGER,
        payload: { ...value, skipList, isRollback: true },
      },
    });
    const output$ = deleteWorkspaceEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createZookeeper.REQUEST,
      },
      b: {
        type: actions.createZookeeper.SUCCESS,
        payload: zookeeperEntity,
      },
      m: {
        type: actions.startZookeeper.REQUEST,
      },
      n: {
        type: actions.startZookeeper.SUCCESS,
        payload: { ...zookeeperEntity, state: 'RUNNING' },
      },
      c: {
        type: actions.createBroker.REQUEST,
      },
      d: {
        type: actions.createBroker.SUCCESS,
        payload: brokerEntity,
      },
      p: {
        type: actions.startBroker.REQUEST,
      },
      q: {
        type: actions.startBroker.SUCCESS,
        payload: { ...brokerEntity, state: 'RUNNING' },
      },
      e: {
        type: actions.createWorker.REQUEST,
      },
      f: {
        type: actions.createWorker.SUCCESS,
        payload: workerEntity,
      },
      r: {
        type: actions.startWorker.REQUEST,
      },
      s: {
        type: actions.startWorker.SUCCESS,
        payload: { ...workerEntity, state: 'RUNNING' },
      },
      t: {
        type: actions.deleteWorkspace.REQUEST,
      },
      u: {
        type: actions.deleteWorkspace.SUCCESS,
        payload: getId(workspaceKey),
      },
      y: {
        type: actions.switchCreateWorkspaceStep.TRIGGER,
        payload: 0,
      },
      z: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          title: `Successfully Delete workspace ${workspaceKey.name}.`,
          type: LOG_LEVEL.info,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
