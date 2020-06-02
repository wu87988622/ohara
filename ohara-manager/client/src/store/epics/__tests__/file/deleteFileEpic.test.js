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

import { throwError } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import { LOG_LEVEL } from 'const';
import * as fileApi from 'api/fileApi';
import deleteFileEpic from '../../file/deleteFileEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as fileEntity } from 'api/__mocks__/fileApi';

jest.mock('api/fileApi');

const fileId = getId(fileEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('delete file should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 999ms u';
    const subs = '    ^----------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteFile.TRIGGER,
        payload: fileEntity,
      },
    });
    const output$ = deleteFileEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteFile.REQUEST,
        payload: { fileId },
      },
      u: {
        type: actions.deleteFile.SUCCESS,
        payload: { fileId },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete multiple files should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-ab         ';
    const expected = '--ab 998ms uv';
    const subs = '    ^------------';
    const anotherFileEntity = { ...fileEntity, name: 'app.jar' };

    const action$ = hot(input, {
      a: {
        type: actions.deleteFile.TRIGGER,
        payload: fileEntity,
      },
      b: {
        type: actions.deleteFile.TRIGGER,
        payload: anotherFileEntity,
      },
    });
    const output$ = deleteFileEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteFile.REQUEST,
        payload: { fileId },
      },
      u: {
        type: actions.deleteFile.SUCCESS,
        payload: { fileId },
      },
      b: {
        type: actions.deleteFile.REQUEST,
        payload: { fileId: getId(anotherFileEntity) },
      },
      v: {
        type: actions.deleteFile.SUCCESS,
        payload: { fileId: getId(anotherFileEntity) },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete same file within period should be deleted once only', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-aa 10s a   ';
    const expected = '--a 999ms u--';
    const subs = '    ^------------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteFile.TRIGGER,
        payload: fileEntity,
      },
    });
    const output$ = deleteFileEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteFile.REQUEST,
        payload: { fileId },
      },
      u: {
        type: actions.deleteFile.SUCCESS,
        payload: { fileId },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of delete file should also trigger event log action', () => {
  const spyCreate = jest.spyOn(fileApi, 'remove').mockReturnValueOnce(
    throwError({
      status: -1,
      data: {},
      title: 'mock delete file failed',
    }),
  );

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.deleteFile.TRIGGER,
        payload: fileEntity,
      },
    });
    const output$ = deleteFileEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteFile.REQUEST,
        payload: { fileId },
      },
      e: {
        type: actions.deleteFile.FAILURE,
        payload: {
          status: -1,
          data: {},
          title: 'mock delete file failed',
        },
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          status: -1,
          data: {},
          title: 'mock delete file failed',
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyCreate).toHaveBeenCalled();
  });
});
