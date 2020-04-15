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

import updateSettingsEpic from '../eventLog/updateSettingsEpic';
import * as actions from 'store/actions';

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  jest.restoreAllMocks();
});

it('update settings should be executed correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a--b--|';
    const expected = '--a--b--|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.updateSettings.TRIGGER,
        payload: {
          limit: 99,
          unlimited: false,
        },
      },
      b: {
        type: actions.updateSettings.TRIGGER,
        payload: {
          unlimited: true,
        },
      },
    });
    const output$ = updateSettingsEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateSettings.SUCCESS,
        payload: {
          limit: 99,
          unlimited: false,
        },
      },
      b: {
        type: actions.updateSettings.SUCCESS,
        payload: { unlimited: true },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
