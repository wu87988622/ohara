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
import updateTopicEpic from '../../topic/updateTopicEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as topicEntity } from 'api/__mocks__/topicApi';

jest.mock('api/topicApi');

const topicId = getId(topicEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('update topic should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.updateTopic.TRIGGER,
        payload: { ...topicEntity, numberOfPartitions: 2 },
      },
    });
    const output$ = updateTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      u: {
        type: actions.updateTopic.SUCCESS,
        payload: {
          topicId,
          entities: {
            topics: {
              [topicId]: { ...topicEntity, numberOfPartitions: 2 },
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

it('update topic multiple times should got latest result', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 60ms c 10s            ';
    const expected = '--a-b 60ms d 36ms u-v 60ms w';
    const subs = '    ^---------------------------';

    const action$ = hot(input, {
      a: {
        type: actions.updateTopic.TRIGGER,
        payload: topicEntity,
      },
      b: {
        type: actions.updateTopic.TRIGGER,
        payload: { ...topicEntity, partitionInfos: ['n1', 'n2'] },
      },
      c: {
        type: actions.updateTopic.TRIGGER,
        payload: { ...topicEntity, numberOfReplications: 1234 },
      },
    });
    const output$ = updateTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      b: {
        type: actions.updateTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      d: {
        type: actions.updateTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      u: {
        type: actions.updateTopic.SUCCESS,
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
      v: {
        type: actions.updateTopic.SUCCESS,
        payload: {
          topicId,
          entities: {
            topics: {
              [topicId]: {
                ...topicEntity,
                partitionInfos: ['n1', 'n2'],
              },
            },
          },
          result: topicId,
        },
      },
      w: {
        type: actions.updateTopic.SUCCESS,
        payload: {
          topicId,
          entities: {
            topics: {
              [topicId]: {
                ...topicEntity,
                numberOfReplications: 1234,
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

it('throw exception of update topic should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock update topic failed',
  };
  const spyCreate = jest
    .spyOn(topicApi, 'update')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.updateTopic.TRIGGER,
        payload: topicEntity,
      },
    });
    const output$ = updateTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateTopic.REQUEST,
        payload: { topicId },
      },
      e: {
        type: actions.updateTopic.FAILURE,
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
