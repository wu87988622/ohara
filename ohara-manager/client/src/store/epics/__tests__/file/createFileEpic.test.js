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

import { of, throwError } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import * as fileApi from 'api/fileApi';
import createFileEpic from '../../file/createFileEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as fileEntity } from 'api/__mocks__/fileApi';
import { delay } from 'rxjs/operators';
import { LOG_LEVEL } from 'const';

jest.mock('api/fileApi');

const fileId = getId(fileEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('create file should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a         ';
    const expected = '-- 500ms a 1999ms u';
    const subs = '    ^-----------';

    jest.spyOn(fileApi, 'getAll').mockReturnValue(
      of({
        status: 200,
        title: 'Get file mock',
        data: [],
      }).pipe(delay(500)),
    );

    const action$ = hot(input, {
      a: {
        type: actions.createFile.TRIGGER,
        payload: fileEntity,
      },
    });
    const output$ = createFileEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createFile.REQUEST,
      },
      u: {
        type: actions.createFile.SUCCESS,
        payload: {
          entities: {
            files: {
              [fileId]: fileEntity,
            },
          },
          result: fileId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('create multiple files should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-ab                                ';
    const expected = '-- 500ms a 1999ms u 499ms b 1999ms v';
    const subs = '    ^-----------------------------------';
    const anotherFileEntity = { ...fileEntity, name: 'app.jar' };

    const action$ = hot(input, {
      a: {
        type: actions.createFile.TRIGGER,
        payload: fileEntity,
      },
      b: {
        type: actions.createFile.TRIGGER,
        payload: anotherFileEntity,
      },
    });
    const output$ = createFileEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createFile.REQUEST,
      },
      u: {
        type: actions.createFile.SUCCESS,
        payload: {
          entities: {
            files: {
              [fileId]: fileEntity,
            },
          },
          result: fileId,
        },
      },
      b: {
        type: actions.createFile.REQUEST,
      },
      v: {
        type: actions.createFile.SUCCESS,
        payload: {
          entities: {
            files: {
              [getId(anotherFileEntity)]: anotherFileEntity,
            },
          },
          result: getId(anotherFileEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('create same file within period should be rename', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    jest.restoreAllMocks();

    const input = '   ^-a                ';
    const expected = '-- 500ms a 1999ms u';
    const subs = '    ^------------------';
    const anotherFileEntity = {
      ...fileEntity,
      name: 'ohara-it-stream1.jar',
      file: new File([], 'ohara-it-stream1.jar'),
    };

    const action$ = hot(input, {
      a: {
        type: actions.createFile.TRIGGER,
        payload: fileEntity,
      },
    });
    const output$ = createFileEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createFile.REQUEST,
      },
      u: {
        type: actions.createFile.SUCCESS,
        payload: {
          entities: {
            files: {
              [getId(anotherFileEntity)]: anotherFileEntity,
            },
          },
          result: getId(anotherFileEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of create file should also trigger event log action', () => {
  const spyCreate = jest.spyOn(fileApi, 'create').mockReturnValueOnce(
    throwError({
      status: -1,
      data: {},
      title: 'mock create file failed',
    }),
  );

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----------|';
    const expected = '-- 500ms (aeu|)';
    const subs = '    ^-------------!';

    const action$ = hot(input, {
      a: {
        type: actions.createFile.TRIGGER,
        payload: fileEntity,
      },
    });
    const output$ = createFileEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createFile.REQUEST,
      },
      e: {
        type: actions.createFile.FAILURE,
        payload: {
          status: -1,
          data: {},
          title: 'mock create file failed',
        },
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          status: -1,
          data: {},
          title: 'mock create file failed',
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyCreate).toHaveBeenCalled();
  });
});
