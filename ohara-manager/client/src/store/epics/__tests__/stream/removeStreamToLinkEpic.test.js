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
import * as streamApi from 'api/streamApi';
import removeStreamToLinkEpic from '../../stream/removeStreamToLinkEpic';
import { ENTITY_TYPE } from 'store/schema';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as streamEntity } from 'api/__mocks__/streamApi';

jest.mock('api/streamApi');
const mockedPaperApi = jest.fn(() => {
  return {
    addLink: () => noop(),
  };
});
const paperApi = new mockedPaperApi();

const streamId = getId(streamEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('remove sink link of stream should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.removeStreamToLink.TRIGGER,
        payload: {
          params: { ...streamEntity, jmxPort: 999 },
          options: { paperApi },
        },
      },
    });
    const output$ = removeStreamToLinkEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.removeStreamToLink.REQUEST,
      },
      u: {
        type: actions.removeStreamToLink.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.streams]: {
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

it('remove stream sink link multiple times should got latest result', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 60ms c 10s        ';
    const expected = '--a-b 60ms d 39ms 60ms w';
    const subs = '    ^-----------------------';

    const action$ = hot(input, {
      a: {
        type: actions.removeStreamToLink.TRIGGER,
        payload: {
          params: streamEntity,
          options: { paperApi },
        },
      },
      b: {
        type: actions.removeStreamToLink.TRIGGER,
        payload: {
          params: { ...streamEntity, nodeNames: ['n1', 'n2'] },
          options: { paperApi },
        },
      },
      c: {
        type: actions.removeStreamToLink.TRIGGER,
        payload: {
          params: { ...streamEntity, clientPort: 1234 },
          options: { paperApi },
        },
      },
    });
    const output$ = removeStreamToLinkEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.removeStreamToLink.REQUEST,
      },
      b: {
        type: actions.removeStreamToLink.REQUEST,
      },
      d: {
        type: actions.removeStreamToLink.REQUEST,
      },
      w: {
        type: actions.removeStreamToLink.SUCCESS,
        payload: {
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

it('throw exception of remove stream to link should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock remove stream to link failed',
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
        type: actions.removeStreamToLink.TRIGGER,
        payload: {
          params: streamEntity,
          options: { paperApi, topic: {} },
        },
      },
    });
    const output$ = removeStreamToLinkEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.removeStreamToLink.REQUEST,
      },
      e: {
        type: actions.removeStreamToLink.FAILURE,
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
