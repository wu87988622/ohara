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

import { of } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';
import { keyBy } from 'lodash';

import * as topicApi from 'api/topicApi';
import stopTopicsEpic from '../../topic/stopTopicsEpic';
import { entity as workspaceEntity } from 'api/__mocks__/workspaceApi';
import { entities as topicEntities } from 'api/__mocks__/topicApi';
import { getKey, getId } from 'utils/object';
import * as actions from 'store/actions';
import { ENTITY_TYPE } from 'store/schema';

jest.mock('api/pipelineApi');
jest.mock('api/topicApi');

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

const workspaceKey = getKey(workspaceEntity);

let spyGetAllTopics;
let spyStopTopic;

beforeEach(() => {
  // ensure the mock data is as expected before each test
  jest.restoreAllMocks();

  spyGetAllTopics = jest.spyOn(topicApi, 'getAll').mockReturnValue(
    of({
      status: 200,
      title: 'mock get all topics',
      data: topicEntities,
    }),
  );

  spyStopTopic = jest.spyOn(topicApi, 'stop');
});

it('stop topics should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 199ms v';
    const subs = ['   ^----------', '--^ 199ms !'];

    const action$ = hot(input, {
      a: {
        type: actions.stopTopics.TRIGGER,
        payload: {
          values: {
            workspaceKey,
          },
        },
      },
    });
    const output$ = stopTopicsEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopTopics.REQUEST,
      },
      v: {
        type: actions.stopTopics.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.topics]: keyBy(topicEntities, (e) => getId(e)),
          },
          result: topicEntities.map((e) => getId(e)),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyGetAllTopics).toHaveBeenCalledTimes(1);
    expect(spyStopTopic).toHaveBeenCalledTimes(3);
  });
});
