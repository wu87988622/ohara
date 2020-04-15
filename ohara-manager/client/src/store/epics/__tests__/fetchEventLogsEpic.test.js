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

import fetchEventLogsEpic from '../eventLog/fetchEventLogsEpic';
import * as actions from 'store/actions';
import localForage from 'localforage';
import { of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { LOG_LEVEL } from 'const';

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  jest.restoreAllMocks();
});

it('fetch event logs should be executed correctly', () => {
  const now = new Date();
  const getEventLog = key => ({
    key,
    type: LOG_LEVEL.info,
    title: `mock event log of ${key}`,
    createAt: now,
    payload: {},
  });

  const spyKeys = jest.spyOn(localForage, 'keys');
  const spyGetItem = jest.spyOn(localForage, 'getItem');
  spyKeys.mockReturnValue(of(['k1', 'k2', 'k3']));
  spyGetItem.mockImplementation(key => of(getEventLog(key)));

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---|';
    const expected = '--a---|';
    const subs = '    ^-----!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchEventLogs.TRIGGER,
        payload: {},
      },
    });
    const output$ = fetchEventLogsEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchEventLogs.SUCCESS,
        payload: [getEventLog('k1'), getEventLog('k2'), getEventLog('k3')],
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyKeys).toHaveBeenCalledTimes(1);
    expect(spyGetItem).toHaveBeenCalledTimes(3);
  });
});

it('multiple fetch actions within period should be executed the latest one', () => {
  const now = new Date();
  const getEventLog = key => ({
    key,
    type: LOG_LEVEL.info,
    title: `mock event log of ${key}`,
    createAt: now,
    payload: {},
  });

  const spyKeys = jest.spyOn(localForage, 'keys');
  const spyGetItem = jest.spyOn(localForage, 'getItem');
  spyKeys.mockReturnValue(of(['k1', 'k2', 'k3']).pipe(delay(10)));
  spyGetItem.mockImplementation(key => of(getEventLog(key)).pipe(delay(5)));

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a----a---------|';
    const expected = '-------- 14ms (a|)';
    const subs = '    ^----------------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchEventLogs.TRIGGER,
        payload: {},
      },
    });
    const output$ = fetchEventLogsEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchEventLogs.SUCCESS,
        payload: [getEventLog('k1'), getEventLog('k2'), getEventLog('k3')],
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyKeys).toHaveBeenCalledTimes(2);
    expect(spyGetItem).toHaveBeenCalledTimes(3);
  });
});
