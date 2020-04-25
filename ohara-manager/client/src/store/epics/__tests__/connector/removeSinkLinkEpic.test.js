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

import removeSinkLinkEpic from '../../connector/removeSinkLinkEpic';
import { ENTITY_TYPE } from 'store/schema';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as connectorEntity } from 'api/__mocks__/connectorApi';
import { noop } from 'rxjs';

jest.mock('api/connectorApi');
const paperApiClass = jest.fn(() => {
  return {
    addLink: () => noop(),
  };
});
const paperApi = new paperApiClass();

const connectorId = getId(connectorEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('remove sink link of connector should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.removeConnectorSinkLink.TRIGGER,
        payload: {
          params: { ...connectorEntity, jmxPort: 999 },
          options: { paperApi },
        },
      },
    });
    const output$ = removeSinkLinkEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.removeConnectorSinkLink.REQUEST,
      },
      u: {
        type: actions.removeConnectorSinkLink.SUCCESS,
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

it('remove connector sink link multiple times should got latest result', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 60ms c 10s        ';
    const expected = '--a-b 60ms d 39ms 60ms w';
    const subs = '    ^-----------------------';

    const action$ = hot(input, {
      a: {
        type: actions.removeConnectorSinkLink.TRIGGER,
        payload: {
          params: connectorEntity,
          options: { paperApi },
        },
      },
      b: {
        type: actions.removeConnectorSinkLink.TRIGGER,
        payload: {
          params: { ...connectorEntity, nodeNames: ['n1', 'n2'] },
          options: { paperApi },
        },
      },
      c: {
        type: actions.removeConnectorSinkLink.TRIGGER,
        payload: {
          params: { ...connectorEntity, clientPort: 1234 },
          options: { paperApi },
        },
      },
    });
    const output$ = removeSinkLinkEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.removeConnectorSinkLink.REQUEST,
      },
      b: {
        type: actions.removeConnectorSinkLink.REQUEST,
      },
      d: {
        type: actions.removeConnectorSinkLink.REQUEST,
      },
      w: {
        type: actions.removeConnectorSinkLink.SUCCESS,
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
