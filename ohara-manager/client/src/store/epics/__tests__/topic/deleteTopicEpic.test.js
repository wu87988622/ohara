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
import deleteTopicEpic from '../../topic/deleteTopicEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as topicEntity } from 'api/__mocks__/topicApi';

jest.mock('api/topicApi');

const topicId = getId(topicEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('should delete a topic', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a            ';
    const expected = '--a 999ms (uv)';
    const subs = '    ^--------------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteTopic.TRIGGER,
        payload: topicEntity,
      },
    });
    const output$ = deleteTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      u: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      v: {
        type: actions.deleteTopic.SUCCESS,
        payload: {
          topicId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('should delete multiple topics', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---b               ';
    const expected = '--a---b 995ms (uv)(xy)';
    const subs = '    ^---------------------';
    const anotherTopicEntity = { ...topicEntity, name: 'bk01' };

    const action$ = hot(input, {
      a: {
        type: actions.deleteTopic.TRIGGER,
        payload: topicEntity,
      },
      b: {
        type: actions.deleteTopic.TRIGGER,
        payload: anotherTopicEntity,
      },
    });
    const output$ = deleteTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      u: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      v: {
        type: actions.deleteTopic.SUCCESS,
        payload: {
          topicId,
        },
      },
      b: {
        type: actions.deleteTopic.REQUEST,
        payload: {
          topicId: getId(anotherTopicEntity),
        },
      },
      x: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      y: {
        type: actions.deleteTopic.SUCCESS,
        payload: {
          topicId: getId(anotherTopicEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete same topic within period should be created once only', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-aa 10s a----';
    const expected = '--a 999ms (uv)';
    const subs = '    ^-------------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteTopic.TRIGGER,
        payload: topicEntity,
      },
    });
    const output$ = deleteTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteTopic.REQUEST,
        payload: {
          topicId,
        },
      },
      u: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      v: {
        type: actions.deleteTopic.SUCCESS,
        payload: {
          topicId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of delete topic should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock delete topic failed',
  };
  const spyCreate = jest
    .spyOn(topicApi, 'remove')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.deleteTopic.TRIGGER,
        payload: topicEntity,
      },
    });
    const output$ = deleteTopicEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteTopic.REQUEST,
        payload: { topicId },
      },
      e: {
        type: actions.deleteTopic.FAILURE,
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
