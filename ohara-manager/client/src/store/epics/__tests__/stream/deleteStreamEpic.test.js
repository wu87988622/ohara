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

import { noop, throwError } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import { LOG_LEVEL } from 'const';
import * as streamApi from 'api/streamApi';
import deleteStreamEpic from '../../stream/deleteStreamEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as streamEntity } from 'api/__mocks__/streamApi';

jest.mock('api/streamApi');
const mockedPaperApi = jest.fn(() => {
  return {
    updateElement: () => noop(),
    removeElement: () => noop(),
  };
});
const paperApi = new mockedPaperApi();

const streamId = getId(streamEntity);

beforeEach(() => {
  // ensure the mock data is as expected before each test
  jest.restoreAllMocks();
});

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('should delete a stream', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a           ';
    const expected = '--a 999ms (uv)';
    const subs = '    ^-------------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteStream.TRIGGER,
        payload: {
          params: streamEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = deleteStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteStream.REQUEST,
        payload: {
          streamId,
        },
      },
      u: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      v: {
        type: actions.deleteStream.SUCCESS,
        payload: {
          streamId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('should delete multiple streams', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---b               ';
    const expected = '--a---b 995ms (uv)(xy)';
    const subs = '    ^---------------------';
    const anotherStreamEntity = {
      ...streamEntity,
      name: 'anotherstream',
    };

    const action$ = hot(input, {
      a: {
        type: actions.deleteStream.TRIGGER,
        payload: {
          params: streamEntity,
          options: { paperApi },
        },
      },
      b: {
        type: actions.deleteStream.TRIGGER,
        payload: {
          params: anotherStreamEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = deleteStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteStream.REQUEST,
        payload: {
          streamId,
        },
      },
      u: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      v: {
        type: actions.deleteStream.SUCCESS,
        payload: {
          streamId,
        },
      },
      b: {
        type: actions.deleteStream.REQUEST,
        payload: {
          streamId: getId(anotherStreamEntity),
        },
      },
      x: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      y: {
        type: actions.deleteStream.SUCCESS,
        payload: {
          streamId: getId(anotherStreamEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete same stream within period should be created once only', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-aa 10s a------';
    const expected = '--a 999ms (uv)--';
    const subs = '    ^---------------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteStream.TRIGGER,
        payload: {
          params: streamEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = deleteStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteStream.REQUEST,
        payload: {
          streamId,
        },
      },
      u: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      v: {
        type: actions.deleteStream.SUCCESS,
        payload: {
          streamId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of delete stream should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock delete stream failed',
  };
  const spyCreate = jest
    .spyOn(streamApi, 'remove')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.deleteStream.TRIGGER,
        payload: {
          params: streamEntity,
          options: { paperApi },
        },
      },
    });
    const output$ = deleteStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteStream.REQUEST,
        payload: { streamId },
      },
      e: {
        type: actions.deleteStream.FAILURE,
        payload: { ...error, streamId },
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          ...error,
          streamId,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyCreate).toHaveBeenCalled();
  });
});
