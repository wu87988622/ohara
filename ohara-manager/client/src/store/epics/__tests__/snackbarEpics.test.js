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

import { showMessageEpic, hideMessageEpic } from '../snackbarEpics';
import * as actions from 'store/actions';

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  jest.restoreAllMocks();
});

it('show message should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---|';
    const expected = '--a---|';
    const subs = '    ^-----!';

    const action$ = hot(input, {
      a: {
        type: actions.showMessage.TRIGGER,
        payload: 'testing message',
      },
    });
    const output$ = showMessageEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.showMessage.SUCCESS,
        payload: {
          message: 'testing message',
          isOpen: true,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('show message multiple times should be throttled', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 500ms a|';
    const expected = '--a-- 500ms a|';
    const subs = '    ^---- 500ms -!';

    const action$ = hot(input, {
      a: {
        type: actions.showMessage.TRIGGER,
        payload: 'testing message',
      },
      b: {
        type: actions.showMessage.TRIGGER,
        payload: 'rapid message again',
      },
    });
    const output$ = showMessageEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.showMessage.SUCCESS,
        payload: {
          message: 'testing message',
          isOpen: true,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('hide message should be worked correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---|';
    const expected = '--a---|';
    const subs = '    ^-----!';

    const action$ = hot(input, {
      a: {
        type: actions.hideMessage.TRIGGER,
        payload: 'testing message',
      },
    });
    const output$ = hideMessageEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.hideMessage.SUCCESS,
        payload: {
          message: '',
          isOpen: false,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
