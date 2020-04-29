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

import removeStreamFromLinkEpic from '../../stream/removeStreamFromLinkEpic';
import { ENTITY_TYPE } from 'store/schema';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as streamEntity } from 'api/__mocks__/streamApi';
import { noop } from 'rxjs';

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

it('remove source link of stream should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.removeStreamFromLink.TRIGGER,
        payload: {
          params: { ...streamEntity, jmxPort: 999 },
          options: { paperApi },
        },
      },
    });
    const output$ = removeStreamFromLinkEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.removeStreamFromLink.REQUEST,
      },
      u: {
        type: actions.removeStreamFromLink.SUCCESS,
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

it('remove stream source link multiple times should got latest result', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 60ms c 10s        ';
    const expected = '--a-b 60ms d 39ms 60ms w';
    const subs = '    ^-----------------------';

    const action$ = hot(input, {
      a: {
        type: actions.removeStreamFromLink.TRIGGER,
        payload: {
          params: streamEntity,
          options: { paperApi },
        },
      },
      b: {
        type: actions.removeStreamFromLink.TRIGGER,
        payload: {
          params: { ...streamEntity, nodeNames: ['n1', 'n2'] },
          options: { paperApi },
        },
      },
      c: {
        type: actions.removeStreamFromLink.TRIGGER,
        payload: {
          params: { ...streamEntity, clientPort: 1234 },
          options: { paperApi },
        },
      },
    });
    const output$ = removeStreamFromLinkEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.removeStreamFromLink.REQUEST,
      },
      b: {
        type: actions.removeStreamFromLink.REQUEST,
      },
      d: {
        type: actions.removeStreamFromLink.REQUEST,
      },
      w: {
        type: actions.removeStreamFromLink.SUCCESS,
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
