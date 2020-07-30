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

import { omit } from 'lodash';
import { TestScheduler } from 'rxjs/testing';
import { of } from 'rxjs';

import { LOG_LEVEL, CELL_STATUS } from 'const';
import startStreamEpic from '../../stream/startStreamEpic';
import * as streamApi from 'api/streamApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as streamEntity } from 'api/__mocks__/streamApi';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';

jest.mock('api/streamApi');
const paperApi = {
  updateElement: jest.fn(),
  removeElement: jest.fn(),
  getCell: jest.fn(),
};

const streamId = getId(streamEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  jest.restoreAllMocks();
  jest.resetAllMocks();
});

it('should start the stream', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 499ms v';
    const subs = '    ^----------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.startStream.TRIGGER,
        payload: {
          params: { ...streamEntity, id },
          options: { paperApi },
        },
      },
    });
    const output$ = startStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startStream.REQUEST,
        payload: {
          streamId,
        },
      },
      v: {
        type: actions.startStream.SUCCESS,
        payload: {
          streamId,
          entities: {
            streams: {
              [streamId]: {
                ...streamEntity,
                id,
                state: SERVICE_STATE.RUNNING,
              },
            },
          },
          result: streamId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(paperApi.updateElement).toHaveBeenCalledTimes(2);
    expect(paperApi.updateElement).toHaveBeenCalledWith(id, {
      status: CELL_STATUS.pending,
    });
    expect(paperApi.updateElement).toHaveBeenCalledWith(id, {
      status: CELL_STATUS.running,
    });
  });
});

it('should fail after reaching the retry limit', () => {
  // mock a 20 times "failed started" result
  const spyGet = jest.spyOn(streamApi, 'get');
  for (let i = 0; i < 20; i++) {
    spyGet.mockReturnValueOnce(
      of({
        status: 200,
        title: 'retry mock get data',
        data: { ...omit(streamEntity) },
      }),
    );
  }
  // get result finally
  spyGet.mockReturnValueOnce(
    of({
      status: 200,
      title: 'retry mock get data',
      data: { ...streamEntity, state: SERVICE_STATE.RUNNING },
    }),
  );

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a            ';
    // we failed after retry 5 times (5 * 2000ms = 10s)
    const expected = '--a 9999ms (vz)';
    const subs = '    ^--------------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.startStream.TRIGGER,
        payload: {
          params: { ...streamEntity, id },
          options: { paperApi },
        },
      },
    });
    const output$ = startStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startStream.REQUEST,
        payload: {
          streamId,
        },
      },
      v: {
        type: actions.startStream.FAILURE,
        payload: {
          streamId,
          data: streamEntity,
          meta: undefined,
          title: `Try to start stream: "${streamEntity.name}" failed after retry 5 times. Expected state: RUNNING, Actual state: undefined`,
        },
      },
      z: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          streamId,
          data: streamEntity,
          meta: undefined,
          title: `Try to start stream: "${streamEntity.name}" failed after retry 5 times. Expected state: RUNNING, Actual state: undefined`,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(paperApi.updateElement).toHaveBeenCalledTimes(2);
    expect(paperApi.updateElement).toHaveBeenCalledWith(id, {
      status: CELL_STATUS.pending,
    });
    expect(paperApi.updateElement).toHaveBeenCalledWith(id, {
      status: CELL_STATUS.stopped,
    });
  });
});

it('start stream multiple times should be worked once', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---a 1s a 10s ';
    const expected = '--a       499ms v';
    const subs = '    ^----------------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.startStream.TRIGGER,
        payload: {
          params: { ...streamEntity, id },
          options: { paperApi },
        },
      },
    });
    const output$ = startStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startStream.REQUEST,
        payload: { streamId },
      },
      v: {
        type: actions.startStream.SUCCESS,
        payload: {
          streamId,
          entities: {
            streams: {
              [streamId]: {
                ...streamEntity,
                id,
                state: SERVICE_STATE.RUNNING,
              },
            },
          },
          result: streamId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(paperApi.updateElement).toHaveBeenCalledTimes(2);
    expect(paperApi.updateElement).toHaveBeenCalledWith(id, {
      status: CELL_STATUS.pending,
    });
    expect(paperApi.updateElement).toHaveBeenCalledWith(id, {
      status: CELL_STATUS.running,
    });
  });
});

it('start different stream should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
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
    const id1 = '1234';
    const id2 = '5678';

    const action$ = hot(input, {
      a: {
        type: actions.startStream.TRIGGER,
        payload: {
          params: { ...streamEntity, id: id1 },
          options: { paperApi },
        },
      },
      b: {
        type: actions.startStream.TRIGGER,
        payload: {
          params: { ...anotherStreamEntity, id: id2 },
          options: { paperApi },
        },
      },
    });
    const output$ = startStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startStream.REQUEST,
        payload: {
          streamId,
        },
      },
      b: {
        type: actions.startStream.REQUEST,
        payload: {
          streamId: getId(anotherStreamEntity),
        },
      },
      y: {
        type: actions.startStream.SUCCESS,
        payload: {
          streamId,
          entities: {
            streams: {
              [streamId]: {
                ...streamEntity,
                id: id1,
                state: SERVICE_STATE.RUNNING,
              },
            },
          },
          result: streamId,
        },
      },
      z: {
        type: actions.startStream.SUCCESS,
        payload: {
          streamId: getId(anotherStreamEntity),
          entities: {
            streams: {
              [getId(anotherStreamEntity)]: {
                ...anotherStreamEntity,
                id: id2,
                state: SERVICE_STATE.RUNNING,
              },
            },
          },
          result: getId(anotherStreamEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(paperApi.updateElement).toHaveBeenCalledTimes(4);
    expect(paperApi.updateElement).toHaveBeenCalledWith(id1, {
      status: CELL_STATUS.pending,
    });
    expect(paperApi.updateElement).toHaveBeenCalledWith(id1, {
      status: CELL_STATUS.running,
    });
  });
});
