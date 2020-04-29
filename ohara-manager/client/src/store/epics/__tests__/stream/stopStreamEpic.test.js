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
import { of, noop } from 'rxjs';

import { LOG_LEVEL } from 'const';
import stopStreamEpic from '../../stream/stopStreamEpic';
import * as streamApi from 'api/streamApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as streamEntity } from 'api/__mocks__/streamApi';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';

jest.mock('api/streamApi');
const mockedPaperApi = jest.fn(() => {
  return {
    updateElement: () => noop(),
    removeElement: () => noop(),
  };
});
const paperApi = new mockedPaperApi();

const streamId = getId(streamEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  // ensure the mock data is as expected before each test
  jest.restoreAllMocks();
});

it('stop stream should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 499ms v';
    const subs = '    ^----------';

    const action$ = hot(input, {
      a: {
        type: actions.stopStream.TRIGGER,
        payload: {
          params: streamEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = stopStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopStream.REQUEST,
        payload: {
          streamId,
        },
      },
      v: {
        type: actions.stopStream.SUCCESS,
        payload: {
          streamId,
          entities: {
            streams: {
              [streamId]: {
                ...streamEntity,
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

it('stop stream failed after reach retry limit', () => {
  // mock a 20 times "failed stoped" result
  const spyGet = jest.spyOn(streamApi, 'get');
  for (let i = 0; i < 20; i++) {
    spyGet.mockReturnValueOnce(
      of({
        status: 200,
        title: 'retry mock get data',
        data: { state: SERVICE_STATE.RUNNING },
      }),
    );
  }
  // get result finally
  spyGet.mockReturnValueOnce(
    of({
      status: 200,
      title: 'retry mock get data',
      data: { ...streamEntity },
    }),
  );

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a            ';
    // we failed after retry 5 times (5 * 2000ms = 10s)
    const expected = '--a 9999ms (vz)';
    const subs = '    ^--------------';

    const action$ = hot(input, {
      a: {
        type: actions.stopStream.TRIGGER,
        payload: {
          params: streamEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = stopStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopStream.REQUEST,
        payload: {
          streamId,
        },
      },
      v: {
        type: actions.stopStream.FAILURE,
        payload: { streamId, title: 'stop stream exceeded max retry count' },
      },
      z: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          streamId,
          title: 'stop stream exceeded max retry count',
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('stop stream multiple times should be worked once', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---a 1s a 10s ';
    const expected = '--a       499ms v';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.stopStream.TRIGGER,
        payload: {
          params: streamEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = stopStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopStream.REQUEST,
        payload: { streamId },
      },
      v: {
        type: actions.stopStream.SUCCESS,
        payload: {
          streamId,
          entities: {
            streams: {
              [streamId]: {
                ...streamEntity,
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

it('stop different stream should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const anotherStreamEntity = {
      ...streamEntity,
      name: 'anotherstream',
      group: 'default',
      xms: 1111,
      xmx: 2222,
      clientPort: 3333,
    };
    const input = '   ^-a--b           ';
    const expected = '--a--b 496ms y--z';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.stopStream.TRIGGER,
        payload: {
          params: streamEntity,
          options: { paperApi },
        },
      },
      b: {
        type: actions.stopStream.TRIGGER,
        payload: {
          params: anotherStreamEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = stopStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopStream.REQUEST,
        payload: {
          streamId,
        },
      },
      b: {
        type: actions.stopStream.REQUEST,
        payload: {
          streamId: getId(anotherStreamEntity),
        },
      },
      y: {
        type: actions.stopStream.SUCCESS,
        payload: {
          streamId,
          entities: {
            streams: {
              [streamId]: {
                ...streamEntity,
              },
            },
          },
          result: streamId,
        },
      },
      z: {
        type: actions.stopStream.SUCCESS,
        payload: {
          streamId: getId(anotherStreamEntity),
          entities: {
            streams: {
              [getId(anotherStreamEntity)]: {
                ...anotherStreamEntity,
              },
            },
          },
          result: getId(anotherStreamEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
