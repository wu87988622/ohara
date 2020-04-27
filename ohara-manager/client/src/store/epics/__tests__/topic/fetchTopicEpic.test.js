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

import fetchTopicsEpic from '../../topic/fetchTopicsEpic';
import { ENTITY_TYPE } from 'store/schema';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as topicEntity } from 'api/__mocks__/topicApi';

jest.mock('api/topicApi');

const topicId = getId(topicEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('fetch topics should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a----------|';
    const expected = '--a 499ms (u|)';
    const subs = '    ^------------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchTopics.TRIGGER,
      },
    });
    const output$ = fetchTopicsEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchTopics.REQUEST,
      },
      u: {
        type: actions.fetchTopics.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.topics]: {
              [topicId]: topicEntity,
            },
          },
          result: [topicId],
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch topic multiple times within period should be got latest result', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a--b----------|';
    const expected = '--a--b 499ms (u|)';
    const subs = '    ^---------------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchTopics.TRIGGER,
      },
      b: {
        type: actions.fetchTopics.TRIGGER,
      },
    });
    const output$ = fetchTopicsEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchTopics.REQUEST,
      },
      b: {
        type: actions.fetchTopics.REQUEST,
      },
      u: {
        type: actions.fetchTopics.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.topics]: {
              [topicId]: topicEntity,
            },
          },
          result: [topicId],
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
