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
import removeSourceLinkEpic from '../../connector/removeSourceLinkEpic';
import { ENTITY_TYPE } from 'store/schema';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as connectorEntity } from 'api/__mocks__/connectorApi';

jest.mock('api/connectorApi');
const mockedPaperApi = jest.fn(() => {
  return {
    addLink: () => noop(),
  };
});
const paperApi = new mockedPaperApi();

const connectorId = getId(connectorEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('remove source link of connector should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.removeConnectorSourceLink.TRIGGER,
        payload: {
          params: { ...connectorEntity, jmxPort: 999 },
          options: { paperApi },
        },
      },
    });
    const output$ = removeSourceLinkEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.removeConnectorSourceLink.REQUEST,
      },
      u: {
        type: actions.removeConnectorSourceLink.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.connectors]: {
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

it('remove connector source link multiple times should got latest result', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 60ms c 10s        ';
    const expected = '--a-b 60ms d 39ms 60ms w';
    const subs = '    ^-----------------------';

    const action$ = hot(input, {
      a: {
        type: actions.removeConnectorSourceLink.TRIGGER,
        payload: {
          params: connectorEntity,
          options: { paperApi },
        },
      },
      b: {
        type: actions.removeConnectorSourceLink.TRIGGER,
        payload: {
          params: { ...connectorEntity, nodeNames: ['n1', 'n2'] },
          options: { paperApi },
        },
      },
      c: {
        type: actions.removeConnectorSourceLink.TRIGGER,
        payload: {
          params: { ...connectorEntity, clientPort: 1234 },
          options: { paperApi },
        },
      },
    });
    const output$ = removeSourceLinkEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.removeConnectorSourceLink.REQUEST,
      },
      b: {
        type: actions.removeConnectorSourceLink.REQUEST,
      },
      d: {
        type: actions.removeConnectorSourceLink.REQUEST,
      },
      w: {
        type: actions.removeConnectorSourceLink.SUCCESS,
        payload: {
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

it('throw exception of remove source link should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock remove source link failed',
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
        type: actions.removeConnectorSourceLink.TRIGGER,
        payload: {
          params: connectorEntity,
          options: { paperApi, topic: {} },
        },
      },
    });
    const output$ = removeSourceLinkEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.removeConnectorSourceLink.REQUEST,
      },
      e: {
        type: actions.removeConnectorSourceLink.FAILURE,
        payload: { ...error },
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
