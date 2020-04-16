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

import deleteWorkerEpic from '../worker/deleteWorkerEpic';
import { entity as workerEntity } from 'api/__mocks__/workerApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';

jest.mock('api/workerApi');

const wkId = getId(workerEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('delete worker should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 999ms u';
    const subs = '    ^----------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteWorker.TRIGGER,
        payload: workerEntity,
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
  });
});

it('delete multiple workers should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-ab         ';
    const expected = '--ab 998ms uv';
    const subs = '    ^------------';
    const anotherWorkerEntity = { ...workerEntity, name: 'wk01' };

    const action$ = hot(input, {
      a: {
        type: actions.deleteWorker.TRIGGER,
        payload: workerEntity,
      },
      b: {
        type: actions.deleteWorker.TRIGGER,
        payload: anotherWorkerEntity,
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
  });
});

it('delete same worker within period should be created once only', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-aa 10s a---';
    const expected = '--a 999ms u--';
    const subs = '    ^------------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteWorker.TRIGGER,
        payload: workerEntity,
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
  });
});
