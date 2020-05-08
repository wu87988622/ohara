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

import { throwError } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import { LOG_LEVEL } from 'const';
import * as workerApi from 'api/workerApi';
import fetchWorkerEpic from '../../worker/fetchWorkerEpic';
import { entity as workerEntity } from 'api/__mocks__/workerApi';
import { workerInfoEntity } from 'api/__mocks__/inspectApi';
import * as actions from 'store/actions';
import { getId, getKey } from 'utils/object';

jest.mock('api/workerApi');
jest.mock('api/inspectApi');

const key = getKey(workerEntity);
const wkId = getId(workerEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('should fetch a worker', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a 10s     ';
    const expected = '--a 2999ms u';
    const subs = '    ^-----------';

    const action$ = hot(input, {
      a: {
        type: actions.fetchWorker.TRIGGER,
        payload: key,
      },
    });
    const output$ = fetchWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchWorker.REQUEST,
        payload: {
          workerId: wkId,
        },
      },
      u: {
        type: actions.fetchWorker.SUCCESS,
        payload: {
          workerId: wkId,
          entities: {
            workers: {
              [wkId]: { ...workerEntity, ...key },
            },
            infos: {
              [wkId]: { ...workerInfoEntity, ...key },
            },
          },
          result: wkId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch worker multiple times within period should get first result', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const anotherKey = { name: 'anotherwk', group: 'newworkspace' };
    const input = '   ^-a 50ms b   ';
    const expected = '--a 2999ms u-';
    const subs = '    ^------------';

    const action$ = hot(input, {
      a: {
        type: actions.fetchWorker.TRIGGER,
        payload: key,
      },
      b: {
        type: actions.fetchWorker.TRIGGER,
        payload: anotherKey,
      },
    });
    const output$ = fetchWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchWorker.REQUEST,
        payload: {
          workerId: wkId,
        },
      },
      u: {
        type: actions.fetchWorker.SUCCESS,
        payload: {
          workerId: wkId,
          entities: {
            workers: {
              [wkId]: { ...workerEntity, ...key },
            },
            infos: {
              [wkId]: { ...workerInfoEntity, ...key },
            },
          },
          result: wkId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch worker multiple times without period should get latest result', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const anotherKey = { name: 'anotherwk', group: 'newworkspace' };
    const input = '   ^-a 2s b         ';
    const expected = '--a 2s b 2999ms u';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.fetchWorker.TRIGGER,
        payload: key,
      },
      b: {
        type: actions.fetchWorker.TRIGGER,
        payload: anotherKey,
      },
    });
    const output$ = fetchWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchWorker.REQUEST,
        payload: {
          workerId: wkId,
        },
      },
      b: {
        type: actions.fetchWorker.REQUEST,
        payload: {
          workerId: getId(anotherKey),
        },
      },
      u: {
        type: actions.fetchWorker.SUCCESS,
        payload: {
          workerId: getId(anotherKey),
          entities: {
            workers: {
              [getId(anotherKey)]: { ...workerEntity, ...anotherKey },
            },
            infos: {
              [getId(anotherKey)]: { ...workerInfoEntity, ...anotherKey },
            },
          },
          result: getId(anotherKey),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of fetch worker should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock get worker failed',
  };
  const spyCreate = jest
    .spyOn(workerApi, 'get')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchWorker.TRIGGER,
        payload: workerEntity,
      },
    });
    const output$ = fetchWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchWorker.REQUEST,
        payload: { workerId: wkId },
      },
      e: {
        type: actions.fetchWorker.FAILURE,
        payload: { ...error, workerId: wkId },
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          ...error,
          workerId: wkId,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyCreate).toHaveBeenCalled();
  });
});
