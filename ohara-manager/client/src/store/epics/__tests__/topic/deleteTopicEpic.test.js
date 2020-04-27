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

import deleteTopicEpic from '../../topic/deleteTopicEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as topicEntity } from 'api/__mocks__/topicApi';
import { LOG_LEVEL } from 'const';

jest.mock('api/topicApi');

const bkId = getId(topicEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('delete topic should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a           ';
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
          topicId: bkId,
        },
      },
      u: {
        type: actions.deleteTopic.SUCCESS,
        payload: {
          topicId: bkId,
        },
      },
      v: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          status: 200,
          title: 'mock delete topic data',
          type: LOG_LEVEL.info,
          data: {},
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete multiple topics should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---b               ';
    const expected = '--a---b 995ms (uy)(vz)';
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
          topicId: bkId,
        },
      },
      u: {
        type: actions.deleteTopic.SUCCESS,
        payload: {
          topicId: bkId,
        },
      },
      b: {
        type: actions.deleteTopic.REQUEST,
        payload: {
          topicId: getId(anotherTopicEntity),
        },
      },
      v: {
        type: actions.deleteTopic.SUCCESS,
        payload: {
          topicId: getId(anotherTopicEntity),
        },
      },
      y: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          status: 200,
          title: 'mock delete topic data',
          type: LOG_LEVEL.info,
          data: {},
        },
      },
      z: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          status: 200,
          title: 'mock delete topic data',
          type: LOG_LEVEL.info,
          data: {},
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
          topicId: bkId,
        },
      },
      u: {
        type: actions.deleteTopic.SUCCESS,
        payload: {
          topicId: bkId,
        },
      },
      v: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          status: 200,
          title: 'mock delete topic data',
          type: LOG_LEVEL.info,
          data: {},
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
