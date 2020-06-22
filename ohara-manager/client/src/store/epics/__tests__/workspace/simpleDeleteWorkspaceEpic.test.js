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
import * as workspaceApi from 'api/workspaceApi';
import simpleDeleteWorkspaceEpic from '../../workspace/simpleDeleteWorkspaceEpic';
import { entity as workspaceEntity } from 'api/__mocks__/workspaceApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';

jest.mock('api/workspaceApi');

const wkId = getId(workspaceEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('delete workspace should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 999ms (uxy)';
    const subs = ['   ^----------', '--^ 999ms !'];

    const action$ = hot(input, {
      a: {
        type: actions.simpleDeleteWorkspace.TRIGGER,
        payload: { values: workspaceEntity },
      },
    });
    const output$ = simpleDeleteWorkspaceEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.simpleDeleteWorkspace.REQUEST,
        payload: {
          workspaceId: wkId,
        },
      },
      u: {
        type: actions.simpleDeleteWorkspace.SUCCESS,
        payload: {
          workspaceId: wkId,
        },
      },
      x: {
        type: actions.switchWorkspace.TRIGGER,
      },
      y: {
        type: actions.fetchNodes.TRIGGER,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete multiple workspaces should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----b         ';
    const expected = '--a-----b 993ms (uxy)-(vxy)';

    const subs = ['   ^------------', '--^ 999ms !', '--------^ 999ms !'];
    const anotherWorkspaceEntity = {
      ...workspaceEntity,
      name: 'wk01',
    };

    const action$ = hot(input, {
      a: {
        type: actions.simpleDeleteWorkspace.TRIGGER,
        payload: {
          values: workspaceEntity,
        },
      },
      b: {
        type: actions.simpleDeleteWorkspace.TRIGGER,
        payload: {
          values: anotherWorkspaceEntity,
        },
      },
    });
    const output$ = simpleDeleteWorkspaceEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.simpleDeleteWorkspace.REQUEST,
        payload: {
          workspaceId: wkId,
        },
      },
      u: {
        type: actions.simpleDeleteWorkspace.SUCCESS,
        payload: {
          workspaceId: wkId,
        },
      },
      b: {
        type: actions.simpleDeleteWorkspace.REQUEST,
        payload: {
          workspaceId: getId(anotherWorkspaceEntity),
        },
      },
      v: {
        type: actions.simpleDeleteWorkspace.SUCCESS,
        payload: {
          workspaceId: getId(anotherWorkspaceEntity),
        },
      },
      x: {
        type: actions.switchWorkspace.TRIGGER,
      },
      y: {
        type: actions.fetchNodes.TRIGGER,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete same workspace within period should be created once only', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-aa 10s a---';
    const expected = '--a 999ms (uxy)';
    const subs = ['   ^------------', '--^ 999ms !'];

    const action$ = hot(input, {
      a: {
        type: actions.simpleDeleteWorkspace.TRIGGER,
        payload: { values: workspaceEntity },
      },
    });
    const output$ = simpleDeleteWorkspaceEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.simpleDeleteWorkspace.REQUEST,
        payload: {
          workspaceId: wkId,
        },
      },
      u: {
        type: actions.simpleDeleteWorkspace.SUCCESS,
        payload: {
          workspaceId: wkId,
        },
      },
      x: {
        type: actions.switchWorkspace.TRIGGER,
      },
      y: {
        type: actions.fetchNodes.TRIGGER,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of delete workspace should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock delete workspace failed',
  };
  const spyDelete = jest
    .spyOn(workspaceApi, 'remove')
    .mockReturnValue(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = ['   ^-------!', '--(^!)'];

    const action$ = hot(input, {
      a: {
        type: actions.simpleDeleteWorkspace.TRIGGER,
        payload: { values: workspaceEntity },
      },
    });
    const output$ = simpleDeleteWorkspaceEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.simpleDeleteWorkspace.REQUEST,
        payload: { workspaceId: wkId },
      },
      e: {
        type: actions.simpleDeleteWorkspace.FAILURE,
        payload: { ...error, workspaceId: wkId },
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          ...error,
          workspaceId: wkId,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyDelete).toHaveBeenCalled();
  });
});
