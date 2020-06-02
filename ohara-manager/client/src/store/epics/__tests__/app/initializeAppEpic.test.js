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

import { keyBy } from 'lodash';
import { throwError } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import initializeAppEpic from '../../app/initializeAppEpic';
import * as actions from 'store/actions';
import * as workspaceApi from 'api/workspaceApi';

import { entities as workspaceEntities } from 'api/__mocks__/workspaceApi';
import { entities as pipelineEntities } from 'api/__mocks__/pipelineApi';
import { LOG_LEVEL } from 'const';

jest.mock('api/workspaceApi');
jest.mock('api/pipelineApi');

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('initial app correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-|         ';
    const expected = '--------(abc|)';
    const subs = '    ^---!---------';

    const action$ = hot(input, {
      a: {
        type: actions.initializeApp.TRIGGER,
        payload: {},
      },
    });
    const output$ = initializeAppEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.initializeApp.SUCCESS,
        payload: {
          entities: {
            pipelines: keyBy(
              pipelineEntities,
              (obj) => `${obj.group}_${obj.name}`,
            ),
            workspaces: keyBy(
              workspaceEntities,
              (obj) => `${obj.group}_${obj.name}`,
            ),
          },
          result: pipelineEntities.map((obj) => `${obj.group}_${obj.name}`),
        },
      },
      b: {
        type: actions.switchWorkspace.TRIGGER,
        payload: {},
      },
      c: {
        type: actions.updateNotifications.TRIGGER,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('switch to existed workspace correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const switchData = {
      workspaceName: 'workspace1',
    };
    const input = '   ^-a-|         ';
    const expected = '--------(abc|)';
    const subs = '    ^---!---------';

    const action$ = hot(input, {
      a: {
        type: actions.initializeApp.TRIGGER,
        payload: switchData,
      },
    });
    const output$ = initializeAppEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.initializeApp.SUCCESS,
        payload: {
          entities: {
            pipelines: keyBy(
              pipelineEntities,
              (obj) => `${obj.group}_${obj.name}`,
            ),
            workspaces: keyBy(
              workspaceEntities,
              (obj) => `${obj.group}_${obj.name}`,
            ),
          },
          result: pipelineEntities.map((obj) => `${obj.group}_${obj.name}`),
        },
      },
      b: {
        type: actions.switchWorkspace.TRIGGER,
        payload: {
          name: switchData.workspaceName,
        },
      },
      c: {
        type: actions.updateNotifications.TRIGGER,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('switch to existed workspace and pipeline correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const switchData = {
      workspaceName: 'workspace1',
      pipelineName: 'p1',
    };
    const input = '   ^-a-|         ';
    const expected = '--------(abc|)';
    const subs = '    ^---!---------';

    const action$ = hot(input, {
      a: {
        type: actions.initializeApp.TRIGGER,
        payload: switchData,
      },
    });
    const output$ = initializeAppEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.initializeApp.SUCCESS,
        payload: {
          entities: {
            pipelines: keyBy(
              pipelineEntities,
              (obj) => `${obj.group}_${obj.name}`,
            ),
            workspaces: keyBy(
              workspaceEntities,
              (obj) => `${obj.group}_${obj.name}`,
            ),
          },
          result: pipelineEntities.map((obj) => `${obj.group}_${obj.name}`),
        },
      },
      b: {
        type: actions.switchWorkspace.TRIGGER,
        payload: {
          name: switchData.workspaceName,
          pipelineName: switchData.pipelineName,
        },
      },
      c: {
        type: actions.updateNotifications.TRIGGER,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('multiple actions will only used the latest action', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const switchData = {
      workspaceName: 'workspace1',
      pipelineName: 'p1',
    };
    const input = '   ^-a--b-|         ';
    const expected = '-----------(abc|)';
    const subs = '    ^------!---------';

    const action$ = hot(input, {
      a: {
        type: actions.initializeApp.TRIGGER,
        payload: switchData,
      },
      b: {
        type: actions.initializeApp.TRIGGER,
        payload: { ...switchData, workspaceName: 'workspace2' },
      },
    });
    const output$ = initializeAppEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.initializeApp.SUCCESS,
        payload: {
          entities: {
            pipelines: keyBy(
              pipelineEntities,
              (obj) => `${obj.group}_${obj.name}`,
            ),
            workspaces: keyBy(
              workspaceEntities,
              (obj) => `${obj.group}_${obj.name}`,
            ),
          },
          result: pipelineEntities.map((obj) => `${obj.group}_${obj.name}`),
        },
      },
      b: {
        type: actions.switchWorkspace.TRIGGER,
        payload: {
          name: 'workspace2',
          pipelineName: switchData.pipelineName,
        },
      },
      c: {
        type: actions.updateNotifications.TRIGGER,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of initial app should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock get workspace list failed',
  };
  const spyCreate = jest
    .spyOn(workspaceApi, 'getAll')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(eu)--|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.initializeApp.TRIGGER,
        payload: {},
      },
    });
    const output$ = initializeAppEpic(action$);

    expectObservable(output$).toBe(expected, {
      e: {
        type: actions.initializeApp.FAILURE,
        payload: error,
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          ...error,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyCreate).toHaveBeenCalled();
  });
});
