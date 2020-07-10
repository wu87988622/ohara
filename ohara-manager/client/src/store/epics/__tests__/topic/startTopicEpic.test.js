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

import { omit } from 'lodash';
import { of } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import { LOG_LEVEL } from 'const';
import startTopicEpic from '../../topic/startTopicEpic';
import * as topicApi from 'api/topicApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as topicEntity } from 'api/__mocks__/topicApi';

jest.mock('api/topicApi');

const topicId = getId(topicEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  // ensure the mock data is as expected before each test
  jest.restoreAllMocks();
});

it('start topic should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 99ms v';
    const subs = '    ^----------';

    const action$ = hot(input, {
      a: {
        type: actions.startTopic.TRIGGER,
        payload: topicEntity,
      },
    });
    const output$ = startTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
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
                state: 'RUNNING',
              },
            },
          },
          result: topicId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('start topic failed after reach retry limit', () => {
  // mock a 20 times "failed started" result
  const spyGet = jest.spyOn(topicApi, 'get');
  for (let i = 0; i < 20; i++) {
    spyGet.mockReturnValueOnce(
      of({
        status: 200,
        title: 'retry mock get data',
        data: { ...omit(topicEntity, 'state') },
      }),
    );
  }
  // get result finally
  spyGet.mockReturnValueOnce(
    of({
      status: 200,
      title: 'retry mock get data',
      data: { ...topicEntity, state: 'RUNNING' },
    }),
  );

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a             ';
    // we failed after retry 11 times (11 * 2000ms = 22s)
    const expected = '--a 21999ms (vz)';
    const subs = '    ^---------------';

    const action$ = hot(input, {
      a: {
        type: actions.startTopic.TRIGGER,
        payload: topicEntity,
      },
    });
    const output$ = startTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      v: {
        type: actions.startTopic.FAILURE,
        payload: {
          topicId,
          data: topicEntity,
          meta: undefined,
          title: `Try to start topic: "${topicEntity.name}" failed after retry 11 times. Expected state: RUNNING, Actual state: undefined`,
        },
      },
      z: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          topicId,
          data: topicEntity,
          meta: undefined,
          title: `Try to start topic: "${topicEntity.name}" failed after retry 11 times. Expected state: RUNNING, Actual state: undefined`,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('start topic multiple times should be worked once', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---a 1s a 10s ';
    const expected = '--a       99ms v';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.startTopic.TRIGGER,
        payload: topicEntity,
      },
    });
    const output$ = startTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startTopic.REQUEST,
        payload: { topicId },
      },
      v: {
        type: actions.startTopic.SUCCESS,
        payload: {
          topicId,
          entities: {
            topics: {
              [topicId]: {
                ...topicEntity,
                state: 'RUNNING',
              },
            },
          },
          result: topicId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('start different topic should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const anotherTopicEntity = {
      ...topicEntity,
      name: 'anothertopic',
      group: 'default',
      numberOfReplications: 3,
    };
    const input = '   ^-a--b           ';
    const expected = '--a--b 96ms y--z';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.startTopic.TRIGGER,
        payload: topicEntity,
      },
      b: {
        type: actions.startTopic.TRIGGER,
        payload: anotherTopicEntity,
      },
    });
    const output$ = startTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      b: {
        type: actions.startTopic.REQUEST,
        payload: {
          topicId: getId(anotherTopicEntity),
        },
      },
      y: {
        type: actions.startTopic.SUCCESS,
        payload: {
          topicId,
          entities: {
            topics: {
              [topicId]: {
                ...topicEntity,
                state: 'RUNNING',
              },
            },
          },
          result: topicId,
        },
      },
      z: {
        type: actions.startTopic.SUCCESS,
        payload: {
          topicId: getId(anotherTopicEntity),
          entities: {
            topics: {
              [getId(anotherTopicEntity)]: {
                ...anotherTopicEntity,
                state: 'RUNNING',
              },
            },
          },
          result: getId(anotherTopicEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
