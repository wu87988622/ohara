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

import { LOG_LEVEL, CELL_TYPE } from 'const';
import * as shabondiApi from 'api/shabondiApi';
import updateShabondiEpic from '../../shabondi/updateShabondiEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as shabondiEntity } from 'api/__mocks__/shabondiApi';

const shabondiId = getId(shabondiEntity);

jest.mock('api/shabondiApi');
const mockedPaperApi = jest.fn(() => {
  return {
    getCells: () => [
      {
        cellType: CELL_TYPE.LINK,
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
const paperApi = new mockedPaperApi();
const cell = {};
const topics = [];
const connectors = [];

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('update shabondi should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.updateShabondi.TRIGGER,
        payload: {
          values: { ...shabondiEntity, jmxPort: 999 },
          options: { paperApi, cell, topics, connectors },
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
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 60ms c 10s            ';
    const expected = '--a-b 60ms d 36ms u-v 60ms w';
    const subs = '    ^---------------------------';

    const action$ = hot(input, {
      a: {
        type: actions.updateShabondi.TRIGGER,
        payload: {
          values: shabondiEntity,
          options: { paperApi, cell, topics, connectors },
        },
      },
      b: {
        type: actions.updateShabondi.TRIGGER,
        payload: {
          values: { ...shabondiEntity, nodeNames: ['n1', 'n2'] },
          options: { paperApi, cell, topics, connectors },
        },
      },
      c: {
        type: actions.updateShabondi.TRIGGER,
        payload: {
          values: { ...shabondiEntity, clientPort: 1234 },
          options: { paperApi, cell, topics, connectors },
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

it('throw exception of update shabondi should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock update shabondi failed',
  };
  const spyCreate = jest
    .spyOn(shabondiApi, 'update')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.updateShabondi.TRIGGER,
        payload: {
          values: shabondiEntity,
          options: { paperApi, cell },
        },
      },
    });
    const output$ = updateShabondiEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateShabondi.REQUEST,
        payload: { shabondiId },
      },
      e: {
        type: actions.updateShabondi.FAILURE,
        payload: { ...error, shabondiId },
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
