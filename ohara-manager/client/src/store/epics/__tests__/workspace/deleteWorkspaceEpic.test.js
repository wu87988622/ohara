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

import deleteWorkspaceEpic from '../../workspace/deleteWorkspaceEpic';
import { entity as workspaceEntity } from 'api/__mocks__/workspaceApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';

jest.mock('api/workspaceApi');

const workspaceId = getId(workspaceEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('delete workspace should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 999ms u';
    const subs = '    ^----------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteWorkspace.TRIGGER,
        payload: workspaceEntity,
      },
    });
    const output$ = deleteWorkspaceEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteWorkspace.REQUEST,
        payload: {
          workspaceId,
        },
      },
      u: {
        type: actions.deleteWorkspace.SUCCESS,
        payload: {
          workspaceId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete multiple workspaces should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-ab         ';
    const expected = '--ab 998ms uv';
    const subs = '    ^------------';
    const anotherWorkspaceEntity = { ...workspaceEntity, name: 'wk01' };

    const action$ = hot(input, {
      a: {
        type: actions.deleteWorkspace.TRIGGER,
        payload: workspaceEntity,
      },
      b: {
        type: actions.deleteWorkspace.TRIGGER,
        payload: anotherWorkspaceEntity,
      },
    });
    const output$ = deleteWorkspaceEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteWorkspace.REQUEST,
        payload: {
          workspaceId,
        },
      },
      u: {
        type: actions.deleteWorkspace.SUCCESS,
        payload: {
          workspaceId,
        },
      },
      b: {
        type: actions.deleteWorkspace.REQUEST,
        payload: {
          workspaceId: getId(anotherWorkspaceEntity),
        },
      },
      v: {
        type: actions.deleteWorkspace.SUCCESS,
        payload: {
          workspaceId: getId(anotherWorkspaceEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete same workspace within period should be created once only', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-aa 10s a---';
    const expected = '--a 999ms u--';
    const subs = '    ^------------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteWorkspace.TRIGGER,
        payload: workspaceEntity,
      },
    });
    const output$ = deleteWorkspaceEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteWorkspace.REQUEST,
        payload: {
          workspaceId,
        },
      },
      u: {
        type: actions.deleteWorkspace.SUCCESS,
        payload: {
          workspaceId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
