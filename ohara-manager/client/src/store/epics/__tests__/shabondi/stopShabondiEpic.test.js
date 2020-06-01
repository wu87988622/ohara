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
import { of } from 'rxjs';

import stopShabondiEpic from '../../shabondi/stopShabondiEpic';
import * as shabondiApi from 'api/shabondiApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as shabondiEntity } from 'api/__mocks__/shabondiApi';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import { LOG_LEVEL, CELL_STATUS } from 'const';

jest.mock('api/shabondiApi');

const paperApi = {
  updateElement: jest.fn(),
  removeElement: jest.fn(),
};

const shabondiId = getId(shabondiEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  jest.restoreAllMocks();
  jest.resetAllMocks();
});

it('should stop the shabondi', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 499ms v';
    const subs = '    ^----------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.stopShabondi.TRIGGER,
        payload: {
          params: { ...shabondiEntity, id },
          options: { paperApi },
        },
      },
    });
    const output$ = stopShabondiEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopShabondi.REQUEST,
        payload: {
          shabondiId,
        },
      },
      v: {
        type: actions.stopShabondi.SUCCESS,
        payload: {
          shabondiId,
          entities: {
            shabondis: {
              [shabondiId]: {
                ...shabondiEntity,
                id,
              },
            },
          },
          result: shabondiId,
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
      status: CELL_STATUS.stopped,
    });
  });
});

it('should fail after reaching the retry limit', () => {
  // mock a 20 times "failed stoped" result
  const spyGet = jest.spyOn(shabondiApi, 'get');
  for (let i = 0; i < 20; i++) {
    spyGet.mockReturnValueOnce(
      of({
        status: 200,
        title: 'retry mock get data',
        data: { ...shabondiEntity, state: SERVICE_STATE.RUNNING },
      }),
    );
  }
  // get result finally
  spyGet.mockReturnValueOnce(
    of({
      status: 200,
      title: 'retry mock get data',
      data: { ...shabondiEntity },
    }),
  );

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a            ';
    // we failed after retry 5 times (5 * 2000ms = 10s)
    const expected = '--a 9999ms (vu)';
    const subs = '    ^--------------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.stopShabondi.TRIGGER,
        payload: {
          params: { ...shabondiEntity, id },
          options: { paperApi },
        },
      },
    });
    const output$ = stopShabondiEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopShabondi.REQUEST,
        payload: {
          shabondiId,
        },
      },
      v: {
        type: actions.stopShabondi.FAILURE,
        payload: {
          shabondiId,
          data: { ...shabondiEntity, state: SERVICE_STATE.RUNNING },
          meta: undefined,
          title: `Try to stop shabondi: "${shabondiEntity.name}" failed after retry 5 times. Expected state is nonexistent, Actual state: RUNNING`,
        },
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          shabondiId,
          data: { ...shabondiEntity, state: SERVICE_STATE.RUNNING },
          meta: undefined,
          title: `Try to stop shabondi: "${shabondiEntity.name}" failed after retry 5 times. Expected state is nonexistent, Actual state: RUNNING`,
          type: LOG_LEVEL.error,
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

it('stop shabondi multiple times should be worked once', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---a 1s a 10s ';
    const expected = '--a       499ms v';
    const subs = '    ^----------------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.stopShabondi.TRIGGER,
        payload: {
          params: { ...shabondiEntity, id },
          options: { paperApi },
        },
      },
    });
    const output$ = stopShabondiEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopShabondi.REQUEST,
        payload: { shabondiId },
      },
      v: {
        type: actions.stopShabondi.SUCCESS,
        payload: {
          shabondiId,
          entities: {
            shabondis: {
              [shabondiId]: {
                ...shabondiEntity,
                id,
              },
            },
          },
          result: shabondiId,
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
      status: CELL_STATUS.stopped,
    });
  });
});

it('stop different shabondi should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const anotherShabondiEntity = {
      ...shabondiEntity,
      name: 'anothershabondi',
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
        type: actions.stopShabondi.TRIGGER,
        payload: {
          params: { ...shabondiEntity, id: id1 },
          options: { paperApi },
        },
      },
      b: {
        type: actions.stopShabondi.TRIGGER,
        payload: {
          params: { ...anotherShabondiEntity, id: id2 },
          options: { paperApi },
        },
      },
    });
    const output$ = stopShabondiEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopShabondi.REQUEST,
        payload: {
          shabondiId,
        },
      },
      b: {
        type: actions.stopShabondi.REQUEST,
        payload: {
          shabondiId: getId(anotherShabondiEntity),
        },
      },
      y: {
        type: actions.stopShabondi.SUCCESS,
        payload: {
          shabondiId,
          entities: {
            shabondis: {
              [shabondiId]: {
                ...shabondiEntity,
                id: id1,
              },
            },
          },
          result: shabondiId,
        },
      },
      z: {
        type: actions.stopShabondi.SUCCESS,
        payload: {
          shabondiId: getId(anotherShabondiEntity),
          entities: {
            shabondis: {
              [getId(anotherShabondiEntity)]: {
                ...anotherShabondiEntity,
                id: id2,
              },
            },
          },
          result: getId(anotherShabondiEntity),
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
      status: CELL_STATUS.stopped,
    });
  });
});
