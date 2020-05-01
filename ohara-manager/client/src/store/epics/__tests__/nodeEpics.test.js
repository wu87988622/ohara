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

import { of, throwError } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import { LOG_LEVEL } from 'const';
import {
  createNodeEpic,
  updateNodeEpic,
  fetchNodesEpic,
  deleteNodeEpic,
} from '../nodeEpics';
import * as actions from 'store/actions';
import * as nodeApi from 'api/nodeApi';
import { entity as nodeEntity } from 'api/__mocks__/nodeApi';
import { ENTITY_TYPE } from 'store/schema';

jest.mock('api/nodeApi');

const nodeId = nodeEntity.hostname;

beforeEach(() => {
  // ensure the mock data is as expected before each test
  jest.restoreAllMocks();
});

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

// create action
it('create node should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a         ';
    const expected = '--a 1999ms u';
    const subs = '    ^-----------';

    const action$ = hot(input, {
      a: {
        type: actions.createNode.TRIGGER,
        payload: { params: nodeEntity },
      },
    });
    const output$ = createNodeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createNode.REQUEST,
      },
      u: {
        type: actions.createNode.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.nodes]: {
              [nodeId]: nodeEntity,
            },
          },
          result: nodeId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('create multiple nodes should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-ab          ';
    const expected = '--ab 1998ms uv';
    const subs = '    ^-------------';
    const anotherNodeEntity = { ...nodeEntity, hostname: 'node01' };

    const action$ = hot(input, {
      a: {
        type: actions.createNode.TRIGGER,
        payload: { params: nodeEntity },
      },
      b: {
        type: actions.createNode.TRIGGER,
        payload: { params: anotherNodeEntity },
      },
    });
    const output$ = createNodeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createNode.REQUEST,
      },
      u: {
        type: actions.createNode.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.nodes]: {
              [nodeId]: nodeEntity,
            },
          },
          result: nodeId,
        },
      },
      b: {
        type: actions.createNode.REQUEST,
      },
      v: {
        type: actions.createNode.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.nodes]: {
              [anotherNodeEntity.hostname]: anotherNodeEntity,
            },
          },
          result: anotherNodeEntity.hostname,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('create same node within period should be created once only', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-aa 10s a    ';
    const expected = '--a 1999ms u--';
    const subs = '    ^-------------';

    const action$ = hot(input, {
      a: {
        type: actions.createNode.TRIGGER,
        payload: { params: nodeEntity },
      },
    });
    const output$ = createNodeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createNode.REQUEST,
      },
      u: {
        type: actions.createNode.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.nodes]: {
              [nodeId]: nodeEntity,
            },
          },
          result: nodeId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of create node should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock create node failed',
  };
  const spyCreate = jest
    .spyOn(nodeApi, 'create')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.createNode.TRIGGER,
        payload: nodeEntity,
      },
    });
    const output$ = createNodeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createNode.REQUEST,
      },
      e: {
        type: actions.createNode.FAILURE,
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

// update action
it('update node should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.updateNode.TRIGGER,
        payload: { ...nodeEntity, password: 'pwd' },
      },
    });
    const output$ = updateNodeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateNode.REQUEST,
      },
      u: {
        type: actions.updateNode.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.nodes]: {
              [nodeEntity.hostname]: { ...nodeEntity, password: 'pwd' },
            },
          },
          result: nodeEntity.hostname,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('update node multiple times should got latest result', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 60ms c 10s            ';
    const expected = '--a-b 60ms d 36ms u-v 60ms w';
    const subs = '    ^---------------------------';

    const action$ = hot(input, {
      a: {
        type: actions.updateNode.TRIGGER,
        payload: nodeEntity,
      },
      b: {
        type: actions.updateNode.TRIGGER,
        payload: { ...nodeEntity, password: 'fake' },
      },
      c: {
        type: actions.updateNode.TRIGGER,
        payload: { ...nodeEntity, tags: { bar: 'foo' } },
      },
    });
    const output$ = updateNodeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateNode.REQUEST,
      },
      b: {
        type: actions.updateNode.REQUEST,
      },
      d: {
        type: actions.updateNode.REQUEST,
      },
      u: {
        type: actions.updateNode.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.nodes]: {
              [nodeEntity.hostname]: nodeEntity,
            },
          },
          result: nodeEntity.hostname,
        },
      },
      v: {
        type: actions.updateNode.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.nodes]: {
              [nodeEntity.hostname]: {
                ...nodeEntity,
                password: 'fake',
              },
            },
          },
          result: nodeEntity.hostname,
        },
      },
      w: {
        type: actions.updateNode.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.nodes]: {
              [nodeEntity.hostname]: {
                ...nodeEntity,
                tags: { bar: 'foo' },
              },
            },
          },
          result: nodeEntity.hostname,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of update node should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock update node failed',
  };
  const spyCreate = jest
    .spyOn(nodeApi, 'update')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.updateNode.TRIGGER,
        payload: nodeEntity,
      },
    });
    const output$ = updateNodeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateNode.REQUEST,
      },
      e: {
        type: actions.updateNode.FAILURE,
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

// fetch action
it('fetch node list should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--a----u|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchNodes.TRIGGER,
      },
    });
    const output$ = fetchNodesEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchNodes.REQUEST,
      },
      u: {
        type: actions.fetchNodes.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.nodes]: {
              [nodeId]: nodeEntity,
            },
          },
          result: [nodeId],
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch node list multiple times within period should get first result', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a 50ms a   ';
    const expected = '--a----u-----';
    const subs = '    ^------------';

    const action$ = hot(input, {
      a: {
        type: actions.fetchNodes.TRIGGER,
      },
    });
    const output$ = fetchNodesEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchNodes.REQUEST,
      },
      u: {
        type: actions.fetchNodes.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.nodes]: {
              [nodeId]: nodeEntity,
            },
          },
          result: [nodeId],
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of fetch node list should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock fetch node list failed',
  };
  const spyCreate = jest
    .spyOn(nodeApi, 'getAll')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchNodes.TRIGGER,
        payload: nodeEntity,
      },
    });
    const output$ = fetchNodesEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchNodes.REQUEST,
      },
      e: {
        type: actions.fetchNodes.FAILURE,
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

// delete action
it('delete node should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 499ms v';
    const subs = '    ^----------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteNode.TRIGGER,
        payload: nodeEntity.hostname,
      },
    });
    const output$ = deleteNodeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteNode.REQUEST,
      },
      v: {
        type: actions.deleteNode.SUCCESS,
        payload: nodeId,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete node failed after reach retry limit', () => {
  // mock a 20 times "failed deleted" result
  const spyGet = jest.spyOn(nodeApi, 'getAll');
  for (let i = 0; i < 20; i++) {
    spyGet.mockReturnValueOnce(
      of({
        status: 200,
        title: 'retry mock get all data',
        data: [nodeEntity],
      }),
    );
  }
  // get empty result finally
  spyGet.mockReturnValueOnce(
    of({
      status: 200,
      title: 'retry mock get all data',
      data: [],
    }),
  );

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a            ';
    // we failed after retry 5 times (5 * 2000ms = 10s)
    const expected = '--a 9999ms (vu)';
    const subs = '    ^--------------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteNode.TRIGGER,
        payload: nodeEntity.hostname,
      },
    });
    const output$ = deleteNodeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteNode.REQUEST,
      },
      v: {
        type: actions.deleteNode.FAILURE,
        payload: {
          title: 'delete node exceeded max retry count',
        },
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          title: 'delete node exceeded max retry count',
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete node multiple times should be executed once', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---a 1s a 10s ';
    const expected = '--a       499ms v';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteNode.TRIGGER,
        payload: nodeEntity.hostname,
      },
    });
    const output$ = deleteNodeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteNode.REQUEST,
      },
      v: {
        type: actions.deleteNode.SUCCESS,
        payload: nodeId,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete different node should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const anotherNodeEntity = {
      ...nodeEntity,
      hostname: 'newnode',
      user: 'fake',
    };
    const input = '   ^-a--b           ';
    const expected = '--a--b 496ms y--z';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteNode.TRIGGER,
        payload: nodeEntity.hostname,
      },
      b: {
        type: actions.deleteNode.TRIGGER,
        payload: anotherNodeEntity.hostname,
      },
    });
    const output$ = deleteNodeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteNode.REQUEST,
      },
      b: {
        type: actions.deleteNode.REQUEST,
      },
      y: {
        type: actions.deleteNode.SUCCESS,
        payload: nodeId,
      },
      z: {
        type: actions.deleteNode.SUCCESS,
        payload: anotherNodeEntity.hostname,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
