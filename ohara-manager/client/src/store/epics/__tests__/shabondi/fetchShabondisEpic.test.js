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

import { keyBy } from 'lodash';
import { throwError } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import { LOG_LEVEL } from 'const';
import * as shabondiApi from 'api/shabondiApi';
import fetchShabondisEpic from '../../shabondi/fetchShabondisEpic';
import { ENTITY_TYPE } from 'store/schema';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entities } from 'api/__mocks__/shabondiApi';

jest.mock('api/shabondiApi');

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('fetch shabondis should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a----------|';
    const expected = '--a 499ms (u|)';
    const subs = '    ^------------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchShabondis.TRIGGER,
      },
    });
    const output$ = fetchShabondisEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchShabondis.REQUEST,
      },
      u: {
        type: actions.fetchShabondis.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.shabondis]: keyBy(entities, e => getId(e)),
          },
          result: entities.map(e => getId(e)),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch shabondi multiple times within period should be got latest result', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a--b----------|';
    const expected = '--a--b 499ms (u|)';
    const subs = '    ^---------------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchShabondis.TRIGGER,
      },
      b: {
        type: actions.fetchShabondis.TRIGGER,
      },
    });
    const output$ = fetchShabondisEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchShabondis.REQUEST,
      },
      b: {
        type: actions.fetchShabondis.REQUEST,
      },
      u: {
        type: actions.fetchShabondis.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.shabondis]: keyBy(entities, e => getId(e)),
          },
          result: entities.map(e => getId(e)),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of fetch shabondi list should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock fetch shabondi list failed',
  };
  const spyCreate = jest
    .spyOn(shabondiApi, 'getAll')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchShabondis.TRIGGER,
      },
    });
    const output$ = fetchShabondisEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchShabondis.REQUEST,
      },
      e: {
        type: actions.fetchShabondis.FAILURE,
        payload: { ...error },
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          ...error,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyCreate).toHaveBeenCalled();
  });
});
