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

import stopAndDeleteStreamEpic from '../../stream/stopAndDeleteStreamEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as streamEntity } from 'api/__mocks__/streamApi';
import { CELL_STATUS } from 'const';

jest.mock('api/streamApi');

const paperApi = {
  updateElement: jest.fn(),
  removeElement: jest.fn(),
  getCell: jest.fn(),
};

beforeEach(() => {
  jest.restoreAllMocks();
  jest.resetAllMocks();
});

const streamId = getId(streamEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('should stop and then delete the stream', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a                     ';
    const expected = '--a 499ms (mn) 996ms (uv)';
    const subs = '    ^-----------------------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.stopAndDeleteStream.TRIGGER,
        payload: {
          params: { ...streamEntity, id },
          options: { paperApi },
        },
      },
    });
    const output$ = stopAndDeleteStreamEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopStream.REQUEST,
        payload: {
          streamId,
        },
      },
      m: {
        type: actions.stopStream.SUCCESS,
        payload: {
          streamId,
          entities: {
            streams: {
              [streamId]: {
                ...streamEntity,
                id,
              },
            },
          },
          result: streamId,
        },
      },
      n: {
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

    expect(paperApi.updateElement).toHaveBeenCalledTimes(3);
    expect(paperApi.removeElement).toHaveBeenCalledTimes(1);
    expect(paperApi.updateElement).toHaveBeenCalledWith(id, {
      status: CELL_STATUS.pending,
    });
    expect(paperApi.updateElement).toHaveBeenCalledWith(id, {
      status: CELL_STATUS.stopped,
    });
    expect(paperApi.removeElement).toHaveBeenCalledWith(id);
  });
});
