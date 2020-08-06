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
import { omit } from 'lodash';

import * as topicApi from 'api/topicApi';
import createAndStartTopicEpic from '../../topic/createAndStartTopicEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as topicEntity } from 'api/__mocks__/topicApi';
import { CELL_STATUS, LOG_LEVEL } from 'const';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';

jest.mock('api/topicApi');

const paperApi = {
  updateElement: jest.fn(),
  removeElement: jest.fn(),
  getCell: jest.fn(),
};

const promise = { resolve: jest.fn(), reject: jest.fn() };

const topicId = getId(topicEntity);

beforeEach(async () => {
  jest.restoreAllMocks();
  jest.resetAllMocks();
});

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('should be able to create and start a topic', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a                    ';
    const expected = '--a 99ms (mn) 196ms v';
    const subs = '    ^----------------------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.createAndStartTopic.TRIGGER,
        payload: {
          values: { ...topicEntity, id },
          options: { paperApi },
          ...promise,
        },
      },
    });
    const output$ = createAndStartTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      m: {
        type: actions.createTopic.SUCCESS,
        payload: {
          topicId,
          entities: {
            topics: {
              [topicId]: { ...topicEntity, id },
            },
          },
          result: topicId,
        },
      },
      n: {
        type: actions.startTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      v: {
        type: actions.startTopic.SUCCESS,
        payload: {
          topicId,
          entities: {
            topics: {
              [topicId]: {
                ...topicEntity,
                id,
                state: SERVICE_STATE.RUNNING,
              },
            },
          },
          result: topicId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(paperApi.updateElement).toHaveBeenCalledTimes(2);
    expect(paperApi.updateElement).toHaveBeenCalledWith(id, {
      status: CELL_STATUS.running,
    });
    expect(promise.resolve).toHaveBeenCalledTimes(1);
  });
});

it('should handle create error', () => {
  const error = {
    status: -1,
    data: {},
    title: 'Error mock',
  };

  jest.spyOn(topicApi, 'create').mockReturnValue(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a    ';
    const expected = '--(abc)';
    const subs = '    ^------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.createAndStartTopic.TRIGGER,
        payload: {
          values: { ...topicEntity, id },
          options: { paperApi },
          ...promise,
        },
      },
    });
    const output$ = createAndStartTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      b: {
        type: actions.createAndStartTopic.FAILURE,
        payload: error,
      },
      c: {
        type: actions.createEventLog.TRIGGER,
        payload: { ...error, type: LOG_LEVEL.error },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(paperApi.updateElement).toHaveBeenCalledTimes(1);
    expect(paperApi.updateElement).toHaveBeenCalledWith(id, {
      status: CELL_STATUS.pending,
    });

    expect(promise.reject).toHaveBeenCalledTimes(1);
    expect(promise.reject).toHaveBeenCalledWith(error);

    expect(paperApi.removeElement).toHaveBeenCalledTimes(1);
    expect(paperApi.removeElement).toHaveBeenCalledWith(id);
  });
});

it('should handle start error', async () => {
  const spyGet = jest.spyOn(topicApi, 'get');
  for (let i = 0; i < 20; i++) {
    spyGet.mockReturnValueOnce(
      of({
        status: 200,
        title: 'retry mock get data',
        data: { ...omit(topicEntity, 'state') },
      }).pipe(delay(100)),
    );
  }

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a                         ';
    const expected = '--a 99ms (bc) 32196ms (de)';
    const subs = '    ^---------------------------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.createAndStartTopic.TRIGGER,
        payload: {
          values: { ...topicEntity, id, state: undefined },
          options: { paperApi },
          ...promise,
        },
      },
    });
    const output$ = createAndStartTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createTopic.REQUEST,
        payload: { topicId },
      },
      b: {
        type: actions.createTopic.SUCCESS,
        payload: {
          topicId,
          entities: {
            topics: {
              [topicId]: { ...topicEntity, id },
            },
          },
          result: topicId,
        },
      },
      c: {
        type: actions.startTopic.REQUEST,
        payload: { topicId },
      },
      d: {
        type: actions.createAndStartTopic.FAILURE,
        payload: {
          topicId,
          data: topicEntity,
          status: 200,
          title: `Failed to start topic ${topicEntity.name}: Unable to confirm the status of the topic is running`,
        },
      },
      e: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          data: topicEntity,
          status: 200,
          title: `Failed to start topic ${topicEntity.name}: Unable to confirm the status of the topic is running`,
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
      status: CELL_STATUS.stopped,
    });

    expect(promise.reject).toHaveBeenCalledTimes(1);
  });
});
