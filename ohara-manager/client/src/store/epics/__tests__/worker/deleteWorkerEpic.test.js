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
import deleteWorkerEpic from '../../worker/deleteWorkerEpic';
import { entity as workerEntity } from 'api/__mocks__/workerApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';

jest.mock('api/workerApi');

const wkId = getId(workerEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

let spyDeleteWorker;

beforeEach(() => {
  // ensure the mock data is as expected before each test
  jest.restoreAllMocks();
  spyDeleteWorker = jest.spyOn(workerApi, 'remove');
});

it('delete worker should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 99ms u';
    const subs = ['   ^----------', '--^ 99ms !'];

    const action$ = hot(input, {
      a: {
        type: actions.deleteWorker.TRIGGER,
        payload: { values: workerEntity },
      },
    });
    const output$ = deleteWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteWorker.REQUEST,
        payload: {
          workerId: wkId,
        },
      },
      u: {
        type: actions.deleteWorker.SUCCESS,
        payload: {
          workerId: wkId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyDeleteWorker).toHaveBeenCalledTimes(1);
  });
});

it('delete multiple workers should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-ab         ';
    const expected = '--ab 98ms uv';
    const subs = [
      '               ^------------',
      '               --^ 99ms !',
      '               ---^ 99ms !',
    ];
    const anotherWorkerEntity = {
      ...workerEntity,
      name: 'wk01',
    };

    const action$ = hot(input, {
      a: {
        type: actions.deleteWorker.TRIGGER,
        payload: { values: workerEntity },
      },
      b: {
        type: actions.deleteWorker.TRIGGER,
        payload: {
          values: anotherWorkerEntity,
        },
      },
    });
    const output$ = deleteWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteWorker.REQUEST,
        payload: {
          workerId: wkId,
        },
      },
      u: {
        type: actions.deleteWorker.SUCCESS,
        payload: {
          workerId: wkId,
        },
      },
      b: {
        type: actions.deleteWorker.REQUEST,
        payload: {
          workerId: getId(anotherWorkerEntity),
        },
      },
      v: {
        type: actions.deleteWorker.SUCCESS,
        payload: {
          workerId: getId(anotherWorkerEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyDeleteWorker).toHaveBeenCalledTimes(2);
  });
});

it('delete same worker within period should be created once only', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-aa 10s a---';
    const expected = '--a 99ms u--';
    const subs = ['   ^------------', '--^ 99ms !'];

    const action$ = hot(input, {
      a: {
        type: actions.deleteWorker.TRIGGER,
        payload: { values: workerEntity },
      },
    });
    const output$ = deleteWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteWorker.REQUEST,
        payload: {
          workerId: wkId,
        },
      },
      u: {
        type: actions.deleteWorker.SUCCESS,
        payload: {
          workerId: wkId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyDeleteWorker).toHaveBeenCalledTimes(1);
  });
});

it('throw exception of delete worker should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock delete worker failed',
  };

  spyDeleteWorker.mockReturnValue(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    // we need 500 ms to get all connectors and 1000ms to delete all connectors
    const expected = '--(aeu)-|';
    const subs = ['   ^-------!', '--(^!)'];

    const action$ = hot(input, {
      a: {
        type: actions.deleteWorker.TRIGGER,
        payload: { values: workerEntity },
      },
    });
    const output$ = deleteWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteWorker.REQUEST,
        payload: { workerId: wkId },
      },
      e: {
        type: actions.deleteWorker.FAILURE,
        payload: {
          ...error,
          workerId: wkId,
        },
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

    expect(spyDeleteWorker).toHaveBeenCalledTimes(1);
  });
});
