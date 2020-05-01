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
import { of } from 'rxjs';

import stopTopicEpic from '../../topic/stopTopicEpic';
import * as topicApi from 'api/topicApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as topicEntity } from 'api/__mocks__/topicApi';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import { LOG_LEVEL } from 'const';

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

it('stop topic should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 499ms v';
    const subs = '    ^----------';

    const action$ = hot(input, {
      a: {
        type: actions.stopTopic.TRIGGER,
        payload: topicEntity,
      },
    });
    const output$ = stopTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      v: {
        type: actions.stopTopic.SUCCESS,
        payload: {
          topicId,
          entities: {
            topics: {
              [topicId]: {
                ...topicEntity,
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

it('stop topic failed after reach retry limit', () => {
  // mock a 20 times "failed stoped" result
  const spyGet = jest.spyOn(topicApi, 'get');
  for (let i = 0; i < 20; i++) {
    spyGet.mockReturnValueOnce(
      of({
        status: 200,
        title: 'retry mock get data',
        data: { state: SERVICE_STATE.RUNNING },
      }),
    );
  }
  // get result finally
  spyGet.mockReturnValueOnce(
    of({
      status: 200,
      title: 'retry mock get data',
      data: { ...topicEntity },
    }),
  );

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a             ';
    // we failed after retry 11 times (11 * 2000ms = 22s)
    const expected = '--a 21999ms (vy)';
    const subs = '    ^---------------';

    const action$ = hot(input, {
      a: {
        type: actions.stopTopic.TRIGGER,
        payload: topicEntity,
      },
    });
    const output$ = stopTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      v: {
        type: actions.stopTopic.FAILURE,
        payload: { topicId, title: 'stop topic exceeded max retry count' },
      },
      y: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          topicId,
          title: 'stop topic exceeded max retry count',
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('stop topic multiple times should be worked once', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---a 1s a 10s ';
    const expected = '--a       499ms v';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.stopTopic.TRIGGER,
        payload: topicEntity,
      },
    });
    const output$ = stopTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopTopic.REQUEST,
        payload: { topicId },
      },
      v: {
        type: actions.stopTopic.SUCCESS,
        payload: {
          topicId,
          entities: {
            topics: {
              [topicId]: {
                ...topicEntity,
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

it('stop different topic should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const anotherTopicEntity = {
      ...topicEntity,
      name: 'anothertopic',
      group: 'default',
      numberOfPartitions: 10,
    };
    const input = '   ^-a--b           ';
    const expected = '--a--b 496ms y--z';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.stopTopic.TRIGGER,
        payload: topicEntity,
      },
      b: {
        type: actions.stopTopic.TRIGGER,
        payload: anotherTopicEntity,
      },
    });
    const output$ = stopTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      b: {
        type: actions.stopTopic.REQUEST,
        payload: {
          topicId: getId(anotherTopicEntity),
        },
      },
      y: {
        type: actions.stopTopic.SUCCESS,
        payload: {
          topicId,
          entities: {
            topics: {
              [topicId]: {
                ...topicEntity,
              },
            },
          },
          result: topicId,
        },
      },
      z: {
        type: actions.stopTopic.SUCCESS,
        payload: {
          topicId: getId(anotherTopicEntity),
          entities: {
            topics: {
              [getId(anotherTopicEntity)]: {
                ...anotherTopicEntity,
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
