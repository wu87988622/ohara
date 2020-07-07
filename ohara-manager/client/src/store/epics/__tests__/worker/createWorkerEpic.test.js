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
import { times } from 'lodash';

import { LOG_LEVEL, GROUP } from 'const';
import * as workerApi from 'api/workerApi';
import createWorkerEpic from '../../worker/createWorkerEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as workerEntity } from 'api/__mocks__/workerApi';

jest.mock('api/workerApi');

const wkId = getId(workerEntity);
const workspaceKey = { name: workerEntity.name, group: GROUP.WORKSPACE };

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('create worker should be worked correctly', () => {
  const mockResolve = jest.fn();
  const mockReject = jest.fn();

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a            ';
    const expected = '--a 99ms (bu)';
    const subs = ['   ^--------------', '--^ 99ms !'];

    const action$ = hot(input, {
      a: {
        type: actions.createWorker.TRIGGER,
        payload: {
          values: workerEntity,
          resolve: mockResolve,
          reject: mockReject,
        },
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
      b: {
        type: actions.updateWorkspace.TRIGGER,
        payload: {
          values: { ...workspaceKey, worker: workerEntity },
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

    expect(mockResolve).toHaveBeenCalled();
    expect(mockResolve).toHaveBeenCalledWith(workerEntity);
    expect(mockReject).not.toHaveBeenCalled();
  });
});

it('create multiple workers should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---b                ';
    const expected = '--a---b 95ms (xu)(yv)';
    const subs = [
      '               ^----                  ',
      '               --^---- 95ms !       ',
      '               ------^---- 95ms !   ',
    ];
    const anotherWorkerEntity = { ...workerEntity, name: 'wk01' };

    const action$ = hot(input, {
      a: {
        type: actions.createWorker.TRIGGER,
        payload: { values: workerEntity },
      },
      b: {
        type: actions.createWorker.TRIGGER,
        payload: { values: anotherWorkerEntity },
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
      x: {
        type: actions.updateWorkspace.TRIGGER,
        payload: {
          values: { ...workspaceKey, worker: workerEntity },
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
      y: {
        type: actions.updateWorkspace.TRIGGER,
        payload: {
          values: {
            name: anotherWorkerEntity.name,
            group: GROUP.WORKSPACE,
            worker: anotherWorkerEntity,
          },
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
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-aa 10s a       ';
    const expected = '--a 99ms (bu)--';
    const subs = ['    ^---------------', '--^ 99ms !'];

    const action$ = hot(input, {
      a: {
        type: actions.createWorker.TRIGGER,
        payload: { values: workerEntity },
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
      b: {
        type: actions.updateWorkspace.TRIGGER,
        payload: {
          values: { ...workspaceKey, worker: workerEntity },
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
  const spyCreate = jest.spyOn(workerApi, 'create');
  times(10, () => spyCreate.mockReturnValueOnce(throwError(error)));
  const mockResolve = jest.fn();
  const mockReject = jest.fn();

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = ['   ^-------!', '--(^!)'];

    const action$ = hot(input, {
      a: {
        type: actions.createWorker.TRIGGER,
        payload: {
          values: workerEntity,
          resolve: mockResolve,
          reject: mockReject,
        },
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
    expect(mockResolve).not.toHaveBeenCalled();
    expect(mockReject).toHaveBeenCalled();
    expect(mockReject).toHaveBeenCalledWith(error);
  });
});
