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
import updateLinkStreamEpic from '../../stream/updateLinkStreamEpic';
import { ENTITY_TYPE } from 'store/schema';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as streamEntity } from 'api/__mocks__/streamApi';

jest.mock('api/streamApi');
const mockedPaperApi = jest.fn(() => {
  return {
    addLink: () => noop(),
    removeElement: () => noop(),
  };
});
const paperApi = new mockedPaperApi();

const streamId = getId(streamEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('remove sink link of stream should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.updateStreamLink.TRIGGER,
        payload: {
          values: { ...streamEntity, jmxPort: 999 },
          options: { paperApi },
        },
      },
    });
    const output$ = updateLinkStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateStreamLink.REQUEST,
      },
      u: {
        type: actions.updateStreamLink.SUCCESS,
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
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 60ms c 10s        ';
    const expected = '--a-b 60ms d 39ms 60ms w';
    const subs = '    ^-----------------------';

    const action$ = hot(input, {
      a: {
        type: actions.updateStreamLink.TRIGGER,
        payload: {
          values: streamEntity,
          options: { paperApi },
        },
      },
      b: {
        type: actions.updateStreamLink.TRIGGER,
        payload: {
          values: { ...streamEntity, nodeNames: ['n1', 'n2'] },
          options: { paperApi },
        },
      },
      c: {
        type: actions.updateStreamLink.TRIGGER,
        payload: {
          values: { ...streamEntity, clientPort: 1234 },
          options: { paperApi },
        },
      },
    });
    const output$ = updateLinkStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateStreamLink.REQUEST,
      },
      b: {
        type: actions.updateStreamLink.REQUEST,
      },
      d: {
        type: actions.updateStreamLink.REQUEST,
      },
      w: {
        type: actions.updateStreamLink.SUCCESS,
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

it('throw exception of update stream link should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock update stream link failed',
  };
  const spyCreate = jest
    .spyOn(streamApi, 'update')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.updateStreamLink.TRIGGER,
        payload: {
          params: streamEntity,
          options: { paperApi, link: {} },
        },
      },
    });
    const output$ = updateLinkStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateStreamLink.REQUEST,
      },
      e: {
        type: actions.updateStreamLink.FAILURE,
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
