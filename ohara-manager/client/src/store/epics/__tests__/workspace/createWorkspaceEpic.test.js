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

import * as actions from 'store/actions';
import { ENTITY_TYPE } from 'store/schema';
import { getId } from 'utils/object';
import createWorkspaceEpic from '../../workspace/createWorkspaceEpic';
import { entity as zookeeperEntity } from 'api/__mocks__/zookeeperApi';
import { entity as brokerEntity } from 'api/__mocks__/brokerApi';
import { entity as workerEntity } from 'api/__mocks__/workerApi';
import { entity as workspaceEntity } from 'api/__mocks__/workspaceApi';

jest.mock('api/zookeeperApi');
jest.mock('api/brokerApi');
jest.mock('api/workerApi');
jest.mock('api/workspaceApi');

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
  const mockResolve = jest.fn();
  const mockReject = jest.fn();

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a         ';
    const expected = '--a 1999ms b';
    const subs = ['   ^-----------', '--^ 1999ms !'];

    const action$ = hot(input, {
      a: {
        type: actions.createWorkspace.TRIGGER,
        payload: {
          values: {
            ...workspaceEntity,
            zookeeper: zookeeperEntity,
            broker: brokerEntity,
            worker: workerEntity,
          },
          resolve: mockResolve,
          reject: mockReject,
        },
      },
    });
    const state$ = new StateObservable(hot('-'));
    const output$ = createWorkspaceEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createWorkspace.REQUEST,
        payload: {
          workspaceId,
        },
      },
      b: {
        type: actions.createWorkspace.SUCCESS,
        payload: {
          workspaceId,
          entities: {
            [ENTITY_TYPE.workspaces]: {
              [workspaceId]: {
                ...workspaceEntity,
                zookeeper: zookeeperEntity,
                broker: brokerEntity,
                worker: workerEntity,
              },
            },
          },
          result: workspaceId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(mockResolve).toHaveBeenCalled();
    expect(mockReject).not.toHaveBeenCalled();
  });
});
