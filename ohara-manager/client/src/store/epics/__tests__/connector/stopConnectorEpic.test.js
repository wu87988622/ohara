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
import { of, noop } from 'rxjs';

import stopConnectorEpic from '../../connector/stopConnectorEpic';
import * as connectorApi from 'api/connectorApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as connectorEntity } from 'api/__mocks__/connectorApi';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import { LOG_LEVEL } from 'const';

jest.mock('api/connectorApi');
const mockedPaperApi = jest.fn(() => {
  return {
    updateElement: () => noop(),
    removeElement: () => noop(),
  };
});
const paperApi = new mockedPaperApi();

const connectorId = getId(connectorEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  // ensure the mock data is as expected before each test
  jest.restoreAllMocks();
});

it('stop connector should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 499ms v';
    const subs = '    ^----------';

    const action$ = hot(input, {
      a: {
        type: actions.stopConnector.TRIGGER,
        payload: {
          params: connectorEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = stopConnectorEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopConnector.REQUEST,
        payload: {
          connectorId,
        },
      },
      v: {
        type: actions.stopConnector.SUCCESS,
        payload: {
          connectorId,
          entities: {
            connectors: {
              [connectorId]: {
                ...connectorEntity,
              },
            },
          },
          result: connectorId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('stop connector failed after reach retry limit', () => {
  // mock a 20 times "failed stoped" result
  const spyGet = jest.spyOn(connectorApi, 'get');
  for (let i = 0; i < 20; i++) {
    spyGet.mockReturnValueOnce(
      of({
        status: 200,
        title: 'retry mock get data',
        data: { ...connectorEntity, state: SERVICE_STATE.RUNNING },
      }),
    );
  }
  // get result finally
  spyGet.mockReturnValueOnce(
    of({
      status: 200,
      title: 'retry mock get data',
      data: { ...connectorEntity },
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
        type: actions.stopConnector.TRIGGER,
        payload: {
          params: connectorEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = stopConnectorEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopConnector.REQUEST,
        payload: {
          connectorId,
        },
      },
      v: {
        type: actions.stopConnector.FAILURE,
        payload: {
          connectorId,
          data: connectorEntity.tasksStatus,
          meta: undefined,
          title: `Try to stop connector: "${connectorEntity.name}" failed after retry 5 times. Expected state is nonexistent, Actual state: RUNNING`,
        },
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          connectorId,
          data: connectorEntity.tasksStatus,
          meta: undefined,
          title: `Try to stop connector: "${connectorEntity.name}" failed after retry 5 times. Expected state is nonexistent, Actual state: RUNNING`,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('stop connector multiple times should be worked once', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---a 1s a 10s ';
    const expected = '--a       499ms v';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.stopConnector.TRIGGER,
        payload: {
          params: connectorEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = stopConnectorEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopConnector.REQUEST,
        payload: { connectorId },
      },
      v: {
        type: actions.stopConnector.SUCCESS,
        payload: {
          connectorId,
          entities: {
            connectors: {
              [connectorId]: {
                ...connectorEntity,
              },
            },
          },
          result: connectorId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('stop different connector should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const anotherConnectorEntity = {
      ...connectorEntity,
      name: 'anotherconnector',
      group: 'default',
      xms: 1111,
      xmx: 2222,
      clientPort: 3333,
    };
    const input = '   ^-a--b           ';
    const expected = '--a--b 496ms y--z';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.stopConnector.TRIGGER,
        payload: {
          params: connectorEntity,
          options: { paperApi },
        },
      },
      b: {
        type: actions.stopConnector.TRIGGER,
        payload: {
          params: anotherConnectorEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = stopConnectorEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopConnector.REQUEST,
        payload: {
          connectorId,
        },
      },
      b: {
        type: actions.stopConnector.REQUEST,
        payload: {
          connectorId: getId(anotherConnectorEntity),
        },
      },
      y: {
        type: actions.stopConnector.SUCCESS,
        payload: {
          connectorId,
          entities: {
            connectors: {
              [connectorId]: {
                ...connectorEntity,
              },
            },
          },
          result: connectorId,
        },
      },
      z: {
        type: actions.stopConnector.SUCCESS,
        payload: {
          connectorId: getId(anotherConnectorEntity),
          entities: {
            connectors: {
              [getId(anotherConnectorEntity)]: {
                ...anotherConnectorEntity,
              },
            },
          },
          result: getId(anotherConnectorEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
