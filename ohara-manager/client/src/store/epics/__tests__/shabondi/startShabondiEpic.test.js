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

import startShabondiEpic from '../../shabondi/startShabondiEpic';
import * as shabondiApi from 'api/shabondiApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as shabondiEntity } from 'api/__mocks__/shabondiApi';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';

jest.mock('api/shabondiApi');
const mockedPaperApi = jest.fn(() => {
  return {
    updateElement: () => noop(),
    removeElement: () => noop(),
  };
});
const paperApi = new mockedPaperApi();

const shabondiId = getId(shabondiEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  // ensure the mock data is as expected before each test
  jest.restoreAllMocks();
});

it('start shabondi should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 499ms v';
    const subs = '    ^----------';

    const action$ = hot(input, {
      a: {
        type: actions.startShabondi.TRIGGER,
        payload: {
          params: shabondiEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = startShabondiEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startShabondi.REQUEST,
        payload: {
          shabondiId,
        },
      },
      v: {
        type: actions.startShabondi.SUCCESS,
        payload: {
          shabondiId,
          entities: {
            shabondis: {
              [shabondiId]: {
                ...shabondiEntity,
                state: SERVICE_STATE.RUNNING,
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

it('start shabondi failed after reach retry limit', () => {
  // mock a 20 times "failed started" result
  const spyGet = jest.spyOn(shabondiApi, 'get');
  for (let i = 0; i < 20; i++) {
    spyGet.mockReturnValueOnce(
      of({
        status: 200,
        title: 'retry mock get data',
        data: {},
      }),
    );
  }
  // get result finally
  spyGet.mockReturnValueOnce(
    of({
      status: 200,
      title: 'retry mock get data',
      data: { ...shabondiEntity, state: SERVICE_STATE.RUNNING },
    }),
  );

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a          ';
    // we failed after retry 5 times (5 * 2000ms = 10s)
    const expected = '--a 9999ms v';
    const subs = '    ^------------';

    const action$ = hot(input, {
      a: {
        type: actions.startShabondi.TRIGGER,
        payload: {
          params: shabondiEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = startShabondiEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startShabondi.REQUEST,
        payload: {
          shabondiId,
        },
      },
      v: {
        type: actions.startShabondi.FAILURE,
        payload: 'exceed max retry times',
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('start shabondi multiple times should be worked once', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---a 1s a 10s ';
    const expected = '--a       499ms v';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.startShabondi.TRIGGER,
        payload: {
          params: shabondiEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = startShabondiEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startShabondi.REQUEST,
        payload: { shabondiId },
      },
      v: {
        type: actions.startShabondi.SUCCESS,
        payload: {
          shabondiId,
          entities: {
            shabondis: {
              [shabondiId]: {
                ...shabondiEntity,
                state: SERVICE_STATE.RUNNING,
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

it('start different shabondi should be worked correctly', () => {
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

    const action$ = hot(input, {
      a: {
        type: actions.startShabondi.TRIGGER,
        payload: {
          params: shabondiEntity,
          options: { paperApi },
        },
      },
      b: {
        type: actions.startShabondi.TRIGGER,
        payload: {
          params: anotherShabondiEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = startShabondiEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startShabondi.REQUEST,
        payload: {
          shabondiId,
        },
      },
      b: {
        type: actions.startShabondi.REQUEST,
        payload: {
          shabondiId: getId(anotherShabondiEntity),
        },
      },
      y: {
        type: actions.startShabondi.SUCCESS,
        payload: {
          shabondiId,
          entities: {
            shabondis: {
              [shabondiId]: {
                ...shabondiEntity,
                state: SERVICE_STATE.RUNNING,
              },
            },
          },
          result: shabondiId,
        },
      },
      z: {
        type: actions.startShabondi.SUCCESS,
        payload: {
          shabondiId: getId(anotherShabondiEntity),
          entities: {
            shabondis: {
              [getId(anotherShabondiEntity)]: {
                ...anotherShabondiEntity,
                state: SERVICE_STATE.RUNNING,
              },
            },
          },
          result: getId(anotherShabondiEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
