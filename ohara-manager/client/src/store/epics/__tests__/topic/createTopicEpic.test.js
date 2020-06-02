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
import * as topicApi from 'api/topicApi';
import createTopicEpic from '../../topic/createTopicEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as topicEntity } from 'api/__mocks__/topicApi';

jest.mock('api/topicApi');

const topicId = getId(topicEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('should create a topic', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a         ';
    const expected = '--a 1999ms u';
    const subs = '    ^-----------';

    const action$ = hot(input, {
      a: {
        type: actions.createTopic.TRIGGER,
        payload: topicEntity,
      },
    });
    const output$ = createTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      u: {
        type: actions.createTopic.SUCCESS,
        payload: {
          topicId,
          entities: {
            topics: {
              [topicId]: topicEntity,
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

it('should create multiple topics', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-ab          ';
    const expected = '--ab 1998ms uv';
    const subs = '    ^-------------';
    const anotherTopicEntity = { ...topicEntity, name: 'topic2' };

    const action$ = hot(input, {
      a: {
        type: actions.createTopic.TRIGGER,
        payload: topicEntity,
      },
      b: {
        type: actions.createTopic.TRIGGER,
        payload: anotherTopicEntity,
      },
    });
    const output$ = createTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      u: {
        type: actions.createTopic.SUCCESS,
        payload: {
          topicId,
          entities: {
            topics: {
              [topicId]: topicEntity,
            },
          },
          result: topicId,
        },
      },
      b: {
        type: actions.createTopic.REQUEST,
        payload: {
          topicId: getId(anotherTopicEntity),
        },
      },
      v: {
        type: actions.createTopic.SUCCESS,
        payload: {
          topicId: getId(anotherTopicEntity),
          entities: {
            topics: {
              [getId(anotherTopicEntity)]: anotherTopicEntity,
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

it('create same topic within period should be created once only', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-aa 10s a  ';
    const expected = '--a 1999ms u';
    const subs = '    ^-----------';

    const action$ = hot(input, {
      a: {
        type: actions.createTopic.TRIGGER,
        payload: topicEntity,
      },
    });
    const output$ = createTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      u: {
        type: actions.createTopic.SUCCESS,
        payload: {
          topicId,
          entities: {
            topics: {
              [topicId]: topicEntity,
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

it('throw exception of create topic should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock create topic failed',
  };
  const spyCreate = jest
    .spyOn(topicApi, 'create')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.createTopic.TRIGGER,
        payload: topicEntity,
      },
    });
    const output$ = createTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createTopic.REQUEST,
        payload: { topicId },
      },
      e: {
        type: actions.createTopic.FAILURE,
        payload: { ...error, topicId },
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          ...error,
          topicId,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyCreate).toHaveBeenCalled();
  });
});
