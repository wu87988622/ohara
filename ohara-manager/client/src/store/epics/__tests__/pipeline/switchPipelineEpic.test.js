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

import switchPipelineEpic from '../../pipeline/switchPipelineEpic';
import * as actions from 'store/actions';
import { StateObservable } from 'redux-observable';
import { ENTITY_TYPE } from 'store/schema';
import { getId } from 'utils/object';
import { entities as workspaceEntities } from 'api/__mocks__/workspaceApi';
import { entities as pipelineEntities } from 'api/__mocks__/pipelineApi';

const stateValue = {
  ui: {
    workspace: { name: workspaceEntities[0].name },
  },
  entities: {
    [ENTITY_TYPE.workspaces]: {
      [getId(workspaceEntities[0])]: workspaceEntities[0],
    },
    [ENTITY_TYPE.pipelines]: {
      [getId(pipelineEntities[0])]: pipelineEntities[0],
      [getId(pipelineEntities[1])]: pipelineEntities[1],
    },
  },
};

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('switches to a target pipeline if the pipeline exists', () => {
  const history = createBrowserHistory();
  const spyPush = jest.spyOn(history, 'push');

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a----|';
    const expected = '--a----|';
    const subs = '    ^------!';

    const action$ = hot(input, {
      a: {
        type: actions.switchPipeline.TRIGGER,
        payload: {
          name: pipelineEntities[1].name,
          group: pipelineEntities[1].group,
          pipelineName: 'p1',
        },
      },
    });
    const state$ = new StateObservable(hot('-a-', { a: stateValue }));
    const dependencies = { history };
    const output$ = switchPipelineEpic(action$, state$, dependencies);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.switchPipeline.SUCCESS,
        payload: pipelineEntities[1].name,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyPush).toHaveBeenCalledTimes(1);
    expect(spyPush).toHaveBeenCalledWith(
      `/${workspaceEntities[0].name}/${pipelineEntities[1].name}`,
    );
  });
});

it('uses the first pipeline as the default when target pipeline cannot be found', () => {
  const history = createBrowserHistory();
  const spyPush = jest.spyOn(history, 'push');

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a----|';
    const expected = '--a----|';
    const subs = '    ^------!';

    const action$ = hot(input, {
      a: {
        type: actions.switchPipeline.TRIGGER,
      },
    });
    const state$ = new StateObservable(hot('-a-', { a: stateValue }));
    const dependencies = { history };
    const output$ = switchPipelineEpic(action$, state$, dependencies);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.switchPipeline.SUCCESS,
        payload: pipelineEntities[0].name,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyPush).toHaveBeenCalledTimes(1);
    expect(spyPush).toHaveBeenCalledWith(
      `/${workspaceEntities[0].name}/${pipelineEntities[0].name}`,
    );
  });
});

it('switches to the current workspace if no pipeline is available under the current workspace', () => {
  const history = createBrowserHistory();
  const spyPush = jest.spyOn(history, 'push');

  const stateValue = {
    ui: {
      workspace: { name: workspaceEntities[0].name },
    },
    entities: {
      [ENTITY_TYPE.workspaces]: {
        [getId(workspaceEntities[0])]: workspaceEntities[0],
      },
      [ENTITY_TYPE.pipelines]: {},
    },
  };

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a----|';
    const expected = '--a----|';
    const subs = '    ^------!';

    const action$ = hot(input, {
      a: {
        type: actions.switchPipeline.TRIGGER,
      },
    });
    const state$ = new StateObservable(hot('-a-', { a: stateValue }));
    const dependencies = { history };
    const output$ = switchPipelineEpic(action$, state$, dependencies);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.switchPipeline.SUCCESS,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyPush).toHaveBeenCalledTimes(1);
    expect(spyPush).toHaveBeenCalledWith(`/${workspaceEntities[0].name}`);
  });
});
