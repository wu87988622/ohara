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
import updateWorkerEpic from '../../worker/updateWorkerEpic';
import { entity as workerEntity } from 'api/__mocks__/workerApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';

jest.mock('api/workerApi');

const wkId = getId(workerEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('update worker should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.updateWorker.TRIGGER,
        payload: { ...workerEntity, jmxPort: 999 },
      },
    });
    const output$ = updateWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateWorker.REQUEST,
        payload: {
          workerId: wkId,
        },
      },
      u: {
        type: actions.updateWorker.SUCCESS,
        payload: {
          workerId: wkId,
          entities: {
            workers: {
              [wkId]: { ...workerEntity, jmxPort: 999 },
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

it('update worker multiple times should got latest result', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 60ms c 10s            ';
    const expected = '--a-b 60ms d 36ms u-v 60ms w';
    const subs = '    ^---------------------------';

    const action$ = hot(input, {
      a: {
        type: actions.updateWorker.TRIGGER,
        payload: workerEntity,
      },
      b: {
        type: actions.updateWorker.TRIGGER,
        payload: { ...workerEntity, nodeNames: ['n1', 'n2'] },
      },
      c: {
        type: actions.updateWorker.TRIGGER,
        payload: { ...workerEntity, clientPort: 1234 },
      },
    });
    const output$ = updateWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateWorker.REQUEST,
        payload: {
          workerId: wkId,
        },
      },
      b: {
        type: actions.updateWorker.REQUEST,
        payload: {
          workerId: wkId,
        },
      },
      d: {
        type: actions.updateWorker.REQUEST,
        payload: {
          workerId: wkId,
        },
      },
      u: {
        type: actions.updateWorker.SUCCESS,
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
      v: {
        type: actions.updateWorker.SUCCESS,
        payload: {
          workerId: wkId,
          entities: {
            workers: {
              [wkId]: {
                ...workerEntity,
                nodeNames: ['n1', 'n2'],
              },
            },
          },
          result: wkId,
        },
      },
      w: {
        type: actions.updateWorker.SUCCESS,
        payload: {
          workerId: wkId,
          entities: {
            workers: {
              [wkId]: {
                ...workerEntity,
                clientPort: 1234,
              },
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

it('throw exception of update worker should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock update worker failed',
  };
  const spyCreate = jest
    .spyOn(workerApi, 'update')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.updateWorker.TRIGGER,
        payload: workerEntity,
      },
    });
    const output$ = updateWorkerEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateWorker.REQUEST,
        payload: { workerId: wkId },
      },
      e: {
        type: actions.updateWorker.FAILURE,
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
