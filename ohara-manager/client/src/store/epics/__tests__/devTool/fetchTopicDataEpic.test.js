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
import { StateObservable } from 'redux-observable';

import fetchTopicDataEpic from '../../devTool/fetchTopicDataEpic';
import * as inspectApi from 'api/inspectApi';
import * as actions from 'store/actions';
import { topicEntity } from 'api/__mocks__/inspectApi';

jest.mock('api/inspectApi');

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  jest.restoreAllMocks();
});

const stateValues = {
  entities: {
    devTool: {
      topicData: {
        query: {
          name: '',
          limit: 10,
        },
      },
    },
  },
};

it('fetch topic data should be executed correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const state$ = new StateObservable(hot('v', { v: stateValues }));

    const input = '   ^-a-----------|';
    const expected = '--- 4999ms (a|)';
    const subs = '    ^-------------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchDevToolTopicData.TRIGGER,
        payload: { group: 'default' },
      },
    });
    const output$ = fetchTopicDataEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchDevToolTopicData.SUCCESS,
        payload: topicEntity.messages,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch topic data multiple times should be executed the first one until finished', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;
    const spyGetTopicData = jest.spyOn(inspectApi, 'getTopicData');

    const state$ = new StateObservable(hot('v', { v: stateValues }));

    const input = '   ^-a-aa    5s   a-----------|';
    const expected = '--- 4999ms a---- 4999ms (a|)';
    const subs = '    ^-----    5s   ------------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchDevToolTopicData.TRIGGER,
        payload: { group: 'default' },
      },
    });
    const output$ = fetchTopicDataEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchDevToolTopicData.SUCCESS,
        payload: topicEntity.messages,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyGetTopicData).toHaveBeenCalledTimes(2);
  });
});
