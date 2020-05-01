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
import createWorkerEpic from '../../worker/createWorkerEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as workerEntity } from 'api/__mocks__/workerApi';

jest.mock('api/workerApi');

const wkId = getId(workerEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('create worker should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a         ';
    const expected = '--a 1999ms u';
    const subs = '    ^-----------';

    const action$ = hot(input, {
      a: {
        type: actions.createWorker.TRIGGER,
        payload: workerEntity,
      },
    });
    const output$ = createWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createWorker.REQUEST,
        payload: {
          workerId: wkId,
        },
      },
      u: {
        type: actions.createWorker.SUCCESS,
        payload: {
          workerId: wkId,
          entities: {
            workers: {
              [wkId]: workerEntity,
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

it('create multiple workers should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-ab          ';
    const expected = '--ab 1998ms uv';
    const subs = '    ^-------------';
    const anotherWorkerEntity = { ...workerEntity, name: 'wk01' };

    const action$ = hot(input, {
      a: {
        type: actions.createWorker.TRIGGER,
        payload: workerEntity,
      },
      b: {
        type: actions.createWorker.TRIGGER,
        payload: anotherWorkerEntity,
      },
    });
    const output$ = createWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createWorker.REQUEST,
        payload: {
          workerId: wkId,
        },
      },
      u: {
        type: actions.createWorker.SUCCESS,
        payload: {
          workerId: wkId,
          entities: {
            workers: {
              [wkId]: workerEntity,
            },
          },
          result: wkId,
        },
      },
      b: {
        type: actions.createWorker.REQUEST,
        payload: {
          workerId: getId(anotherWorkerEntity),
        },
      },
      v: {
        type: actions.createWorker.SUCCESS,
        payload: {
          workerId: getId(anotherWorkerEntity),
          entities: {
            workers: {
              [getId(anotherWorkerEntity)]: anotherWorkerEntity,
            },
          },
          result: getId(anotherWorkerEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('create same worker within period should be created once only', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-aa 10s a    ';
    const expected = '--a 1999ms u--';
    const subs = '    ^-------------';

    const action$ = hot(input, {
      a: {
        type: actions.createWorker.TRIGGER,
        payload: workerEntity,
      },
    });
    const output$ = createWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createWorker.REQUEST,
        payload: {
          workerId: wkId,
        },
      },
      u: {
        type: actions.createWorker.SUCCESS,
        payload: {
          workerId: wkId,
          entities: {
            workers: {
              [wkId]: workerEntity,
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

it('throw exception of create worker should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock create worker failed',
  };
  const spyCreate = jest
    .spyOn(workerApi, 'create')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.createWorker.TRIGGER,
        payload: workerEntity,
      },
    });
    const output$ = createWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createWorker.REQUEST,
        payload: { workerId: wkId },
      },
      e: {
        type: actions.createWorker.FAILURE,
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
