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

import { LOG_LEVEL } from 'const';
import * as connectorApi from 'api/connectorApi';
import * as actions from 'store/actions';
import deleteConnectorEpic from '../../connector/deleteConnectorEpic';
import { getId } from 'utils/object';
import { entity as connectorEntity } from 'api/__mocks__/connectorApi';

jest.mock('api/connectorApi');
const mockedPaperApi = jest.fn(() => {
  return {
    updateElement: () => noop(),
    removeElement: () => noop(),
  };
});
const paperApi = new mockedPaperApi();

const connectorId = getId(connectorEntity);

beforeEach(() => {
  // ensure the mock data is as expected before each test
  jest.restoreAllMocks();
});

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('should delete a connector', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a           ';
    const expected = '--a 999ms (uv)';
    const subs = '    ^-------------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteConnector.TRIGGER,
        payload: {
          params: connectorEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = deleteConnectorEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteConnector.REQUEST,
        payload: {
          connectorId,
        },
      },
      u: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      v: {
        type: actions.deleteConnector.SUCCESS,
        payload: {
          connectorId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('should delete multiple connectors', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---b               ';
    const expected = '--a---b 995ms (uv)(xy)';
    const subs = '    ^---------------------';
    const anotherConnectorEntity = {
      ...connectorEntity,
      name: 'anotherconnector',
    };

    const action$ = hot(input, {
      a: {
        type: actions.deleteConnector.TRIGGER,
        payload: {
          params: connectorEntity,
          options: { paperApi },
        },
      },
      b: {
        type: actions.deleteConnector.TRIGGER,
        payload: {
          params: anotherConnectorEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = deleteConnectorEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteConnector.REQUEST,
        payload: {
          connectorId,
        },
      },
      u: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      v: {
        type: actions.deleteConnector.SUCCESS,
        payload: {
          connectorId,
        },
      },
      b: {
        type: actions.deleteConnector.REQUEST,
        payload: {
          connectorId: getId(anotherConnectorEntity),
        },
      },
      x: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      y: {
        type: actions.deleteConnector.SUCCESS,
        payload: {
          connectorId: getId(anotherConnectorEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete same connector within period should be created once only', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-aa 10s a----';
    const expected = '--a 999ms (uv)';
    const subs = '    ^-------------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteConnector.TRIGGER,
        payload: {
          params: connectorEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = deleteConnectorEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteConnector.REQUEST,
        payload: {
          connectorId,
        },
      },
      u: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      v: {
        type: actions.deleteConnector.SUCCESS,
        payload: {
          connectorId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of delete connector should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock delete connector failed',
  };
  const spyCreate = jest
    .spyOn(connectorApi, 'remove')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.deleteConnector.TRIGGER,
        payload: {
          params: connectorEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = deleteConnectorEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteConnector.REQUEST,
        payload: { connectorId },
      },
      e: {
        type: actions.deleteConnector.FAILURE,
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
