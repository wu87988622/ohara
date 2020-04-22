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

import { noop } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import updateShabondiEpic from '../../shabondi/updateShabondiEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as shabondiEntity } from 'api/__mocks__/shabondiApi';

const shabondiId = getId(shabondiEntity);

jest.mock('api/shabondiApi');
const paperApiClass = jest.fn(() => {
  return {
    getCells: () => [
      {
        cellType: 'standard.Link',
        sourceId: shabondiId,
        targetId: shabondiId,
      },
    ],
    getCell: () => ({
      id: shabondiId,
    }),
    addLink: () => noop(),
  };
});
const paperApi = new paperApiClass();
const cell = {};

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('update shabondi should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.updateShabondi.TRIGGER,
        payload: {
          params: { ...shabondiEntity, jmxPort: 999 },
          options: { paperApi, cell },
        },
      },
    });
    const output$ = updateShabondiEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateShabondi.REQUEST,
        payload: {
          shabondiId,
        },
      },
      u: {
        type: actions.updateShabondi.SUCCESS,
        payload: {
          shabondiId,
          entities: {
            shabondis: {
              [shabondiId]: { ...shabondiEntity, jmxPort: 999 },
            },
          },
          result: shabondiId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('update shabondi multiple times should got latest result', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 60ms c 10s            ';
    const expected = '--a-b 60ms d 36ms u-v 60ms w';
    const subs = '    ^---------------------------';

    const action$ = hot(input, {
      a: {
        type: actions.updateShabondi.TRIGGER,
        payload: {
          params: shabondiEntity,
          options: { paperApi, cell },
        },
      },
      b: {
        type: actions.updateShabondi.TRIGGER,
        payload: {
          params: { ...shabondiEntity, nodeNames: ['n1', 'n2'] },
          options: { paperApi, cell },
        },
      },
      c: {
        type: actions.updateShabondi.TRIGGER,
        payload: {
          params: { ...shabondiEntity, clientPort: 1234 },
          options: { paperApi, cell },
        },
      },
    });
    const output$ = updateShabondiEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateShabondi.REQUEST,
        payload: {
          shabondiId,
        },
      },
      b: {
        type: actions.updateShabondi.REQUEST,
        payload: {
          shabondiId,
        },
      },
      d: {
        type: actions.updateShabondi.REQUEST,
        payload: {
          shabondiId,
        },
      },
      u: {
        type: actions.updateShabondi.SUCCESS,
        payload: {
          shabondiId,
          entities: {
            shabondis: {
              [shabondiId]: shabondiEntity,
            },
          },
          result: shabondiId,
        },
      },
      v: {
        type: actions.updateShabondi.SUCCESS,
        payload: {
          shabondiId,
          entities: {
            shabondis: {
              [shabondiId]: {
                ...shabondiEntity,
                nodeNames: ['n1', 'n2'],
              },
            },
          },
          result: shabondiId,
        },
      },
      w: {
        type: actions.updateShabondi.SUCCESS,
        payload: {
          shabondiId,
          entities: {
            shabondis: {
              [shabondiId]: {
                ...shabondiEntity,
                clientPort: 1234,
              },
            },
          },
          result: shabondiId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
