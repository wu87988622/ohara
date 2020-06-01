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

import createAndStartTopicEpic from '../../topic/createAndStartTopicEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as topicEntity } from 'api/__mocks__/topicApi';
import { CELL_STATUS } from 'const';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';

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

it('create and start topic should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a                     ';
    const expected = '--a 1999ms (mn) 496ms v';
    const subs = '    ^-----------------------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.createAndStartTopic.TRIGGER,
        payload: {
          params: { ...topicEntity, id },
          options: { paperApi },
          promise,
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
    expect(promise.resolve).toHaveBeenCalledTimes(1);
    expect(paperApi.updateElement).toHaveBeenCalledWith(id, {
      status: CELL_STATUS.running,
    });
  });
});
