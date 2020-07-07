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

import updateWorkspaceEpic from '../../workspace/updateWorkspaceEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as workspaceEntity } from 'api/__mocks__/workspaceApi';

jest.mock('api/workspaceApi');

const workspaceId = getId(workspaceEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('update workspace should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.updateWorkspace.TRIGGER,
        payload: { values: { ...workspaceEntity, bar: 'foo' } },
      },
    });
    const output$ = updateWorkspaceEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateWorkspace.REQUEST,
        payload: {
          workspaceId,
        },
      },
      u: {
        type: actions.updateWorkspace.SUCCESS,
        payload: {
          workspaceId,
          entities: {
            workspaces: {
              [workspaceId]: { ...workspaceEntity, bar: 'foo' },
            },
          },
          result: workspaceId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('should only get the latest result when updating a workspace multiple times', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 60ms c 10s            ';
    const expected = '--a-b 60ms d 36ms u-v 60ms w';
    const subs = '    ^---------------------------';

    const action$ = hot(input, {
      a: {
        type: actions.updateWorkspace.TRIGGER,
        payload: { values: workspaceEntity },
      },
      b: {
        type: actions.updateWorkspace.TRIGGER,
        payload: { values: { ...workspaceEntity, nodeNames: ['n1', 'n2'] } },
      },
      c: {
        type: actions.updateWorkspace.TRIGGER,
        payload: { values: { ...workspaceEntity, nodeNames: ['n3'] } },
      },
    });
    const output$ = updateWorkspaceEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateWorkspace.REQUEST,
        payload: {
          workspaceId,
        },
      },
      b: {
        type: actions.updateWorkspace.REQUEST,
        payload: {
          workspaceId,
        },
      },
      d: {
        type: actions.updateWorkspace.REQUEST,
        payload: {
          workspaceId,
        },
      },
      u: {
        type: actions.updateWorkspace.SUCCESS,
        payload: {
          workspaceId,
          entities: {
            workspaces: {
              [workspaceId]: workspaceEntity,
            },
          },
          result: workspaceId,
        },
      },
      v: {
        type: actions.updateWorkspace.SUCCESS,
        payload: {
          workspaceId,
          entities: {
            workspaces: {
              [workspaceId]: {
                ...workspaceEntity,
                nodeNames: ['n1', 'n2'],
              },
            },
          },
          result: workspaceId,
        },
      },
      w: {
        type: actions.updateWorkspace.SUCCESS,
        payload: {
          workspaceId,
          entities: {
            workspaces: {
              [workspaceId]: {
                ...workspaceEntity,
                nodeNames: ['n3'],
              },
            },
          },
          result: workspaceId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
