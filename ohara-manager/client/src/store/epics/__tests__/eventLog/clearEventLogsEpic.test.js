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

import clearEventLogsEpic from '../../eventLog/clearEventLogsEpic';
import * as actions from 'store/actions';

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  jest.restoreAllMocks();
});

it('clear event log should be executed correctly', () => {
  const spyClear = jest.spyOn(localForage, 'clear');
  spyClear.mockImplementation(() => of(noop()));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---|';
    const expected = '--(ab)|';
    const subs = '    ^-----!';

    const action$ = hot(input, {
      a: {
        type: actions.clearEventLogs.TRIGGER,
      },
    });
    const output$ = clearEventLogsEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.clearEventLogs.SUCCESS,
      },
      b: {
        type: actions.fetchEventLogs.TRIGGER,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyClear).toHaveBeenCalledTimes(1);
  });
});

it('clear event log multiple times should be executed once only', () => {
  const spyClear = jest.spyOn(localForage, 'clear');
  // simulate a 10ms delay "promise-like" function
  spyClear.mockImplementation(() => of(noop()).pipe(delay(10)));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a--a---------|';
    const expected = '------ 9ms (ab|)';
    const subs = '    ^--------------!';

    const action$ = hot(input, {
      a: {
        type: actions.clearEventLogs.TRIGGER,
      },
    });
    const output$ = clearEventLogsEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.clearEventLogs.SUCCESS,
      },
      b: {
        type: actions.fetchEventLogs.TRIGGER,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyClear).toHaveBeenCalledTimes(2);
  });
});
