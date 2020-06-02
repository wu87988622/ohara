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

import fetchFilesEpic from '../../file/fetchFilesEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as fileEntity } from 'api/__mocks__/fileApi';
import { ENTITY_TYPE } from 'store/schema';

jest.mock('api/fileApi');

const fileId = getId(fileEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('fetch file list should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a 10s    ';
    const expected = '--a 499ms u';
    const subs = '    ^----------';

    const action$ = hot(input, {
      a: {
        type: actions.fetchFiles.TRIGGER,
      },
    });
    const output$ = fetchFilesEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchFiles.REQUEST,
      },
      u: {
        type: actions.fetchFiles.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.files]: { [fileId]: fileEntity },
          },
          result: [fileId],
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch file list multiple times within period should get first result', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a 50ms a   ';
    const expected = '--a 499ms u--';
    const subs = '    ^------------';

    const action$ = hot(input, {
      a: {
        type: actions.fetchFiles.TRIGGER,
      },
    });
    const output$ = fetchFilesEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchFiles.REQUEST,
      },
      u: {
        type: actions.fetchFiles.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.files]: { [fileId]: fileEntity },
          },
          result: [fileId],
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
