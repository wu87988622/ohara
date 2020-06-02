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

import { noop, throwError } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import { LOG_LEVEL, CELL_TYPES } from 'const';
import * as connectorApi from 'api/connectorApi';
import updateConnectorEpic from '../../connector/updateConnectorEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as connectorEntity } from 'api/__mocks__/connectorApi';

const connectorId = getId(connectorEntity);

jest.mock('api/connectorApi');
const mockedPaperApi = jest.fn(() => {
  return {
    getCells: () => [
      {
        cellType: CELL_TYPES.LINK,
        sourceId: connectorId,
        targetId: connectorId,
      },
    ],
    getCell: () => ({
      id: connectorId,
    }),
    addLink: () => noop(),
  };
});
const paperApi = new mockedPaperApi();
const cell = {};
const topics = [];
const connectors = [];

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('should update connector', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.updateConnector.TRIGGER,
        payload: {
          values: { ...connectorEntity, jmxPort: 999 },
          options: { paperApi, cell, topics, connectors },
        },
      },
    });
    const output$ = updateConnectorEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateConnector.REQUEST,
        payload: {
          connectorId,
        },
      },
      u: {
        type: actions.updateConnector.SUCCESS,
        payload: {
          connectorId,
          entities: {
            connectors: {
              [connectorId]: { ...connectorEntity, jmxPort: 999 },
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

it('update connector multiple times should got latest result', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 60ms c 10s            ';
    const expected = '--a-b 60ms d 36ms u-v 60ms w';
    const subs = '    ^---------------------------';

    const action$ = hot(input, {
      a: {
        type: actions.updateConnector.TRIGGER,
        payload: {
          values: connectorEntity,
          options: { paperApi, cell, topics, connectors },
        },
      },
      b: {
        type: actions.updateConnector.TRIGGER,
        payload: {
          values: { ...connectorEntity, nodeNames: ['n1', 'n2'] },
          options: { paperApi, cell, topics, connectors },
        },
      },
      c: {
        type: actions.updateConnector.TRIGGER,
        payload: {
          values: { ...connectorEntity, clientPort: 1234 },
          options: { paperApi, cell, topics, connectors },
        },
      },
    });
    const output$ = updateConnectorEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateConnector.REQUEST,
        payload: {
          connectorId,
        },
      },
      b: {
        type: actions.updateConnector.REQUEST,
        payload: {
          connectorId,
        },
      },
      d: {
        type: actions.updateConnector.REQUEST,
        payload: {
          connectorId,
        },
      },
      u: {
        type: actions.updateConnector.SUCCESS,
        payload: {
          connectorId,
          entities: {
            connectors: {
              [connectorId]: connectorEntity,
            },
          },
          result: connectorId,
        },
      },
      v: {
        type: actions.updateConnector.SUCCESS,
        payload: {
          connectorId,
          entities: {
            connectors: {
              [connectorId]: {
                ...connectorEntity,
                nodeNames: ['n1', 'n2'],
              },
            },
          },
          result: connectorId,
        },
      },
      w: {
        type: actions.updateConnector.SUCCESS,
        payload: {
          connectorId,
          entities: {
            connectors: {
              [connectorId]: {
                ...connectorEntity,
                clientPort: 1234,
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

it('throw exception of update connector should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock update connector failed',
  };
  const spyCreate = jest
    .spyOn(connectorApi, 'update')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.updateConnector.TRIGGER,
        payload: {
          values: connectorEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = updateConnectorEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateConnector.REQUEST,
        payload: { connectorId },
      },
      e: {
        type: actions.updateConnector.FAILURE,
        payload: { ...error, connectorId },
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          ...error,
          connectorId,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyCreate).toHaveBeenCalled();
  });
});
