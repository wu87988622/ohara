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
import * as streamApi from 'api/streamApi';
import updateStreamEpic from '../../stream/updateStreamEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as streamEntity } from 'api/__mocks__/streamApi';

const streamId = getId(streamEntity);

jest.mock('api/streamApi');
const mockedPaperApi = jest.fn(() => {
  return {
    getCells: () => [
      {
        cellType: CELL_TYPES.LINK,
        sourceId: streamId,
        targetId: streamId,
      },
    ],
    getCell: () => ({
      id: streamId,
    }),
    addLink: () => noop(),
    removeLink: () => noop(),
  };
});
const paperApi = new mockedPaperApi();
const cell = {};
const streams = [streamEntity];
const topics = [
  { key: 'from', data: { id: streamEntity.from } },
  { key: 'to', data: { id: streamEntity.to } },
];

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('update stream should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.updateStream.TRIGGER,
        payload: {
          values: { ...streamEntity, jmxPort: 999 },
          options: { paperApi, cell, topics, streams },
        },
      },
    });
    const output$ = updateStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateStream.REQUEST,
        payload: {
          streamId,
        },
      },
      u: {
        type: actions.updateStream.SUCCESS,
        payload: {
          streamId,
          entities: {
            streams: {
              [streamId]: { ...streamEntity, jmxPort: 999 },
            },
          },
          result: streamId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('update stream multiple times should got latest result', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 60ms c 10s            ';
    const expected = '--a-b 60ms d 36ms u-v 60ms w';
    const subs = '    ^---------------------------';

    const action$ = hot(input, {
      a: {
        type: actions.updateStream.TRIGGER,
        payload: {
          values: streamEntity,
          options: { paperApi, cell, topics, streams },
        },
      },
      b: {
        type: actions.updateStream.TRIGGER,
        payload: {
          values: { ...streamEntity, nodeNames: ['n1', 'n2'] },
          options: { paperApi, cell, topics, streams },
        },
      },
      c: {
        type: actions.updateStream.TRIGGER,
        payload: {
          values: { ...streamEntity, clientPort: 1234 },
          options: { paperApi, cell, topics, streams },
        },
      },
    });
    const output$ = updateStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateStream.REQUEST,
        payload: {
          streamId,
        },
      },
      b: {
        type: actions.updateStream.REQUEST,
        payload: {
          streamId,
        },
      },
      d: {
        type: actions.updateStream.REQUEST,
        payload: {
          streamId,
        },
      },
      u: {
        type: actions.updateStream.SUCCESS,
        payload: {
          streamId,
          entities: {
            streams: {
              [streamId]: streamEntity,
            },
          },
          result: streamId,
        },
      },
      v: {
        type: actions.updateStream.SUCCESS,
        payload: {
          streamId,
          entities: {
            streams: {
              [streamId]: {
                ...streamEntity,
                nodeNames: ['n1', 'n2'],
              },
            },
          },
          result: streamId,
        },
      },
      w: {
        type: actions.updateStream.SUCCESS,
        payload: {
          streamId,
          entities: {
            streams: {
              [streamId]: {
                ...streamEntity,
                clientPort: 1234,
              },
            },
          },
          result: streamId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of update stream should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock update stream failed',
  };
  const spyCreate = jest
    .spyOn(streamApi, 'update')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.updateStream.TRIGGER,
        payload: {
          values: streamEntity,
          options: { paperApi, cell, topics, streams },
        },
      },
    });
    const output$ = updateStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateStream.REQUEST,
        payload: { streamId },
      },
      e: {
        type: actions.updateStream.FAILURE,
        payload: { ...error, streamId },
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          ...error,
          streamId,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyCreate).toHaveBeenCalled();
  });
});
