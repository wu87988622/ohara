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

import * as topicApi from 'api/topicApi';
import * as actions from 'store/actions';
import stopAndDeleteTopicEpic from '../../topic/stopAndDeleteTopicEpic';
import { getId } from 'utils/object';
import { entity as topicEntity } from 'api/__mocks__/topicApi';
import { CELL_STATUS, LOG_LEVEL } from 'const';

jest.mock('api/topicApi');

const paperApi = {
  updateElement: jest.fn(),
  removeElement: jest.fn(),
};

const promise = { resolve: jest.fn(), reject: jest.fn() };

const topicId = getId(topicEntity);

beforeEach(() => {
  jest.restoreAllMocks();
  jest.resetAllMocks();
});

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('should able to stop and delete a topic', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a                      ';
    const expected = '--a 99ms (mn) 96ms (vz)';
    const subs = '    ^------------------------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.stopAndDeleteTopic.TRIGGER,
        payload: {
          params: { ...topicEntity, id },
          options: { paperApi },
          promise,
        },
      },
    });
    const output$ = stopAndDeleteTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      m: {
        type: actions.stopTopic.SUCCESS,
        payload: {
          topicId,
          entities: {
            topics: {
              [topicId]: {
                ...topicEntity,
                id,
              },
            },
          },
          result: topicId,
        },
      },
      n: {
        type: actions.deleteTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      v: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      z: {
        type: actions.deleteTopic.SUCCESS,
        payload: {
          topicId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(paperApi.removeElement).toHaveBeenCalledTimes(1);
    expect(paperApi.removeElement).toHaveBeenCalledWith(id);

    expect(paperApi.updateElement).toHaveBeenCalledTimes(1);
    expect(paperApi.updateElement).toHaveBeenCalledWith(id, {
      status: CELL_STATUS.pending,
    });

    expect(promise.resolve).toHaveBeenCalledTimes(1);
  });
});

it('should handle stop error', () => {
  const error = {
    status: -1,
    data: {},
    title: 'Error mock',
  };

  const stopError = {
    data: {},
    meta: undefined,
    title: `Try to stop topic: "${topicEntity.name}" failed after retry 11 times. Expected state is nonexistent, Actual state: undefined`,
  };

  jest.spyOn(topicApi, 'stop').mockReturnValue(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a    ';
    const expected = '--a 21999ms (bc)';
    const subs = '    ^------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.stopAndDeleteTopic.TRIGGER,
        payload: {
          params: { ...topicEntity, id },
          options: { paperApi },
          promise,
        },
      },
    });
    const output$ = stopAndDeleteTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      b: {
        type: actions.stopAndDeleteTopic.FAILURE,
        payload: stopError,
      },
      c: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          ...stopError,
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

    expect(promise.reject).toHaveBeenCalledTimes(1);
    expect(promise.reject).toHaveBeenCalledWith(stopError);
  });
});

it('should handle delete error', async () => {
  const error = {
    status: -1,
    data: {},
    title: 'Error mock',
  };

  jest.spyOn(topicApi, 'remove').mockReturnValue(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a                         ';
    const expected = '--a 99ms (bcde) ';
    const subs = '    ^---------------------------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.stopAndDeleteTopic.TRIGGER,
        payload: {
          params: { ...topicEntity, id },
          options: { paperApi },
          promise,
        },
      },
    });
    const output$ = stopAndDeleteTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopTopic.REQUEST,
        payload: { topicId },
      },
      b: {
        type: actions.stopTopic.SUCCESS,
        payload: {
          topicId,
          entities: {
            topics: {
              [topicId]: {
                ...topicEntity,
                id,
              },
            },
          },
          result: topicId,
        },
      },
      c: {
        type: actions.deleteTopic.REQUEST,
        payload: { topicId },
      },
      d: {
        type: actions.stopAndDeleteTopic.FAILURE,
        payload: error,
      },
      e: {
        type: actions.createEventLog.TRIGGER,
        payload: { ...error, type: LOG_LEVEL.error },
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

    expect(promise.reject).toHaveBeenCalledTimes(1);
    expect(promise.reject).toHaveBeenCalledWith(error);
  });
});
