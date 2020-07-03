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

import createEventLogEpic from '../../eventLog/createEventLogEpic';
import * as actions from 'store/actions';
import localForage from 'localforage';
import { of } from 'rxjs';
import { LOG_LEVEL } from 'const';

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  jest.restoreAllMocks();
});

it('create event log should be executed correctly', () => {
  const now = new Date();
  const spySetItem = jest.spyOn(localForage, 'setItem');
  jest.spyOn(global, 'Date').mockImplementation(() => now);
  spySetItem.mockImplementation((key, value) => of(value));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a----|';
    const expected = '--(abc)|';
    const subs = '    ^------!';

    const action$ = hot(input, {
      a: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          type: LOG_LEVEL.info,
          title: 'mock create event',
          data: { bar: 'foo' },
        },
      },
    });
    const output$ = createEventLogEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createEventLog.SUCCESS,
        payload: {
          key: now.getTime().toString(),
          type: LOG_LEVEL.info,
          title: 'mock create event',
          createAt: now,
          payload: { bar: 'foo' },
        },
      },
      b: {
        type: actions.showMessage.TRIGGER,
        payload: 'mock create event',
      },
      c: {
        type: actions.updateNotifications.TRIGGER,
        payload: {
          key: now.getTime().toString(),
          type: LOG_LEVEL.info,
          title: 'mock create event',
          createAt: now,
          payload: { bar: 'foo' },
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spySetItem).toHaveBeenCalledTimes(1);
  });
});

it('create multiple event logs should be executed correctly', () => {
  const now = new Date();
  const spySetItem = jest.spyOn(localForage, 'setItem');
  jest.spyOn(global, 'Date').mockImplementation(() => now);
  spySetItem.mockImplementation((key, value) => of(value));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a----a    2s      b----|';
    const expected = '--(aut)(aut) 1996ms (bvs)|';
    const subs = '    ^-------    2s      -----!';

    const action$ = hot(input, {
      a: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          type: LOG_LEVEL.info,
          title: 'mock create event',
          data: { bar: 'foo' },
        },
      },
      b: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          type: LOG_LEVEL.error,
          title: 'mock create error log!',
          data: { baz: 'hello' },
        },
      },
    });
    const output$ = createEventLogEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createEventLog.SUCCESS,
        payload: {
          key: now.getTime().toString(),
          type: LOG_LEVEL.info,
          title: 'mock create event',
          createAt: now,
          payload: { bar: 'foo' },
        },
      },
      u: {
        type: actions.showMessage.TRIGGER,
        payload: 'mock create event',
      },
      t: {
        type: actions.updateNotifications.TRIGGER,
        payload: {
          key: now.getTime().toString(),
          type: LOG_LEVEL.info,
          title: 'mock create event',
          createAt: now,
          payload: { bar: 'foo' },
        },
      },
      b: {
        type: actions.createEventLog.SUCCESS,
        payload: {
          key: now.getTime().toString(),
          type: LOG_LEVEL.error,
          title: 'mock create error log!',
          createAt: now,
          payload: { baz: 'hello' },
        },
      },
      s: {
        type: actions.updateNotifications.TRIGGER,
        payload: {
          key: now.getTime().toString(),
          type: LOG_LEVEL.error,
          title: 'mock create error log!',
          createAt: now,
          payload: { baz: 'hello' },
        },
      },
      v: {
        type: actions.showMessage.TRIGGER,
        payload: 'mock create error log!',
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spySetItem).toHaveBeenCalledTimes(3);
  });
});
