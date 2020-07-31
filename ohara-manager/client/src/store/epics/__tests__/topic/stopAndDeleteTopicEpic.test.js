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

import { of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { TestScheduler } from 'rxjs/testing';

import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
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
  getCell: jest.fn(),
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

it('should be able to stop and delete a topic', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a                      ';
    const expected = '--a 199ms (mn) 96ms (vz)';
    const subs = '    ^------------------------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.stopAndDeleteTopic.TRIGGER,
        payload: {
          values: { ...topicEntity, id },
          options: { paperApi },
          ...promise,
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

    expect(paperApi.updateElement).toHaveBeenCalledTimes(2);
    expect(paperApi.updateElement).toHaveBeenCalledWith(id, {
      status: CELL_STATUS.pending,
    });

    expect(promise.resolve).toHaveBeenCalledTimes(1);
  });
});

it('should handle stop error', () => {
  const spyGet = jest.spyOn(topicApi, 'get');
  for (let i = 0; i < 20; i++) {
    spyGet.mockReturnValueOnce(
      of({
        status: 200,
        title: 'retry mock get data',
        data: { ...topicEntity, state: SERVICE_STATE.RUNNING },
      }).pipe(delay(100)),
    );
  }

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a    ';
    const expected = '--a 32199ms (bc)';
    const subs = '    ^------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.stopAndDeleteTopic.TRIGGER,
        payload: {
          values: { ...topicEntity, id },
          options: { paperApi },
          ...promise,
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
        payload: {
          topicId,
          data: {
            ...topicEntity,
            state: SERVICE_STATE.RUNNING,
          },
          status: 200,
          title: `Failed to stop topic ${topicEntity.name}: Unable to confirm the status of the topic is not running`,
        },
      },
      c: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          topicId,
          data: {
            ...topicEntity,
            state: SERVICE_STATE.RUNNING,
          },
          status: 200,
          title: `Failed to stop topic ${topicEntity.name}: Unable to confirm the status of the topic is not running`,
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
    const expected = '--a 199ms (bcde) ';
    const subs = '    ^---------------------------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.stopAndDeleteTopic.TRIGGER,
        payload: {
          values: { ...topicEntity, id },
          options: { paperApi },
          ...promise,
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
        payload: { ...error, topicId, type: LOG_LEVEL.error },
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
