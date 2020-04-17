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
import { of, noop } from 'rxjs';
import { delay } from 'rxjs/operators';
import localForage from 'localforage';

import deleteEventLogsEpic from '../../eventLog/deleteEventLogsEpic';
import * as actions from 'store/actions';

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  jest.restoreAllMocks();
});

it('remove event logs should be executed correctly', () => {
  const spyRemoveItem = jest.spyOn(localForage, 'removeItem');
  spyRemoveItem.mockImplementation(() => of(noop()));

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---|';
    const expected = '--a---|';
    const subs = '    ^-----!';

    const action$ = hot(input, {
      a: {
        type: actions.deleteEventLogs.TRIGGER,
        payload: ['k1', 'k2', 'k3'],
      },
    });
    const output$ = deleteEventLogsEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteEventLogs.SUCCESS,
        payload: ['k1', 'k2', 'k3'],
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyRemoveItem).toHaveBeenCalledTimes(3);
  });
});

it('multiple remove actions within period should be executed the latest one', () => {
  const spyRemoveItem = jest.spyOn(localForage, 'removeItem');
  spyRemoveItem.mockImplementation(() => of(noop()).pipe(delay(5)));

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a--b------|';
    const expected = '----------u-|';
    const subs = '    ^-----------!';

    const action$ = hot(input, {
      a: {
        type: actions.deleteEventLogs.TRIGGER,
        payload: ['k1', 'k2', 'k3'],
      },
      b: {
        type: actions.deleteEventLogs.TRIGGER,
        payload: ['v1', 'v2'],
      },
    });
    const output$ = deleteEventLogsEpic(action$);

    expectObservable(output$).toBe(expected, {
      u: {
        type: actions.deleteEventLogs.SUCCESS,
        payload: ['v1', 'v2'],
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    // although the previous observable had been cancelled
    // the removeItem requests still in fly
    expect(spyRemoveItem).toHaveBeenCalledTimes(5);
  });
});
