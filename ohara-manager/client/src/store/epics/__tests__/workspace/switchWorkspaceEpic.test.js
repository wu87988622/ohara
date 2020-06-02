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
import { createBrowserHistory } from 'history';

import switchWorkspaceEpic from '../../workspace/switchWorkspaceEpic';
import * as actions from 'store/actions';
import { StateObservable } from 'redux-observable';
import { ENTITY_TYPE } from 'store/schema';
import { getId } from 'utils/object';
import { entities as workspaceEntities } from 'api/__mocks__/workspaceApi';

const stateValue = {
  entities: {
    [ENTITY_TYPE.workspaces]: {
      [getId(workspaceEntities[0])]: workspaceEntities[0],
      [getId(workspaceEntities[1])]: workspaceEntities[1],
    },
  },
};

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('switch workspace should be worked correctly', () => {
  const history = createBrowserHistory();
  const spyPush = jest.spyOn(history, 'push');

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a----|';
    const expected = '--(ab)-|';
    const subs = '    ^------!';

    const action$ = hot(input, {
      a: {
        type: actions.switchWorkspace.TRIGGER,
        payload: {
          name: workspaceEntities[1].name,
          group: workspaceEntities[1].group,
          pipelineName: 'p1',
        },
      },
    });
    const state$ = new StateObservable(hot('-a-', { a: stateValue }));
    const dependencies = { history };
    const output$ = switchWorkspaceEpic(action$, state$, dependencies);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.switchWorkspace.SUCCESS,
        payload: workspaceEntities[1].name,
      },
      b: {
        type: actions.switchPipeline.TRIGGER,
        payload: {
          name: 'p1',
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyPush).toHaveBeenCalledTimes(1);
  });
});

it('switch to empty workspace should be redirected to the first workspace', () => {
  const history = createBrowserHistory();
  const spyPush = jest.spyOn(history, 'push');

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a----|';
    const expected = '--(ab)-|';
    const subs = '    ^------!';

    const action$ = hot(input, {
      a: {
        type: actions.switchWorkspace.TRIGGER,
      },
    });
    const state$ = new StateObservable(hot('-a-', { a: stateValue }));
    const dependencies = { history };
    const output$ = switchWorkspaceEpic(action$, state$, dependencies);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.switchWorkspace.SUCCESS,
        payload: workspaceEntities[0].name,
      },
      b: {
        type: actions.switchPipeline.TRIGGER,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyPush).toHaveBeenCalledTimes(1);
  });
});

it('switch to nonexistent workspace should be redirected to the first workspace', () => {
  const history = createBrowserHistory();
  const spyPush = jest.spyOn(history, 'push');

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a----|';
    const expected = '--(ab)-|';
    const subs = '    ^------!';

    const action$ = hot(input, {
      a: {
        type: actions.switchWorkspace.TRIGGER,
        payload: {
          name: 'fake',
          group: 'boo',
        },
      },
    });
    const state$ = new StateObservable(hot('-a-', { a: stateValue }));
    const dependencies = { history };
    const output$ = switchWorkspaceEpic(action$, state$, dependencies);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.switchWorkspace.SUCCESS,
        payload: workspaceEntities[0].name,
      },
      b: {
        type: actions.switchPipeline.TRIGGER,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyPush).toHaveBeenCalledTimes(1);
  });
});

it('redirect to root path if there were no workspace existed', () => {
  const history = createBrowserHistory();
  const spyPush = jest.spyOn(history, 'push');

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a----|';
    const expected = '--(ab)-|';
    const subs = '    ^------!';

    const action$ = hot(input, {
      a: {
        type: actions.switchWorkspace.TRIGGER,
      },
    });
    const state$ = new StateObservable(
      hot('-a-', { a: { entities: { workspaces: {} } } }),
    );
    const dependencies = { history };
    const output$ = switchWorkspaceEpic(action$, state$, dependencies);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.switchWorkspace.SUCCESS,
      },
      b: {
        type: actions.switchPipeline.TRIGGER,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyPush).toHaveBeenCalledTimes(1);
  });
});
