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

import { LOG_LEVEL, CELL_STATUS } from 'const';
import * as shabondiApi from 'api/shabondiApi';
import deleteShabondiEpic from '../../shabondi/deleteShabondiEpic';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as shabondiEntity } from 'api/__mocks__/shabondiApi';

jest.mock('api/shabondiApi');

const paperApi = {
  updateElement: jest.fn(),
  removeElement: jest.fn(),
};

const shabondiId = getId(shabondiEntity);

beforeEach(jest.resetAllMocks);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('should delete a shabondi', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a           ';
    const expected = '--a 999ms (uv)';
    const subs = '    ^-------------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.deleteShabondi.TRIGGER,
        payload: {
          params: { ...shabondiEntity, id },
          options: { paperApi },
        },
      },
    });
    const output$ = deleteShabondiEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteShabondi.REQUEST,
        payload: {
          shabondiId,
        },
      },
      u: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      v: {
        type: actions.deleteShabondi.SUCCESS,
        payload: {
          shabondiId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(paperApi.updateElement).toHaveBeenCalledTimes(1);
    expect(paperApi.removeElement).toHaveBeenCalledTimes(1);
    expect(paperApi.updateElement).toHaveBeenCalledWith(id, {
      status: CELL_STATUS.pending,
    });
    expect(paperApi.removeElement).toHaveBeenCalledWith(id);
  });
});

it('should delete multiple shabondis', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---b               ';
    const expected = '--a---b 995ms (uv)(xy)';
    const subs = '    ^---------------------';
    const anotherShabondiEntity = {
      ...shabondiEntity,
      name: 'anothershabondi',
    };
    const id1 = '1234';
    const id2 = '5678';

    const action$ = hot(input, {
      a: {
        type: actions.deleteShabondi.TRIGGER,
        payload: {
          params: { ...shabondiEntity, id: id1 },
          options: { paperApi },
        },
      },
      b: {
        type: actions.deleteShabondi.TRIGGER,
        payload: {
          params: { ...anotherShabondiEntity, id: id2 },
          options: { paperApi },
        },
      },
    });
    const output$ = deleteShabondiEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteShabondi.REQUEST,
        payload: {
          shabondiId,
        },
      },
      u: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      v: {
        type: actions.deleteShabondi.SUCCESS,
        payload: {
          shabondiId,
        },
      },
      b: {
        type: actions.deleteShabondi.REQUEST,
        payload: {
          shabondiId: getId(anotherShabondiEntity),
        },
      },
      x: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      y: {
        type: actions.deleteShabondi.SUCCESS,
        payload: {
          shabondiId: getId(anotherShabondiEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(paperApi.updateElement).toHaveBeenCalledTimes(2);
    expect(paperApi.removeElement).toHaveBeenCalledTimes(2);
    expect(paperApi.updateElement).toHaveBeenCalledWith(id1, {
      status: CELL_STATUS.pending,
    });
    expect(paperApi.updateElement).toHaveBeenCalledWith(id2, {
      status: CELL_STATUS.pending,
    });
    expect(paperApi.removeElement).toHaveBeenCalledWith(id1);
    expect(paperApi.removeElement).toHaveBeenCalledWith(id2);
  });
});

it(`should not call paperApi when the options params are not supplied`, () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a           ';
    const expected = '--a 999ms (uv)';
    const subs = '    ^-------------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteShabondi.TRIGGER,
        payload: {
          params: shabondiEntity,
        },
      },
    });
    const output$ = deleteShabondiEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteShabondi.REQUEST,
        payload: {
          shabondiId,
        },
      },
      u: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      v: {
        type: actions.deleteShabondi.SUCCESS,
        payload: {
          shabondiId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(paperApi.updateElement).not.toHaveBeenCalled();
    expect(paperApi.removeElement).not.toHaveBeenCalled();
  });
});

it('delete same shabondi within period should be created once only', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-aa 10s a----';
    const expected = '--a 999ms (uv)';
    const subs = '    ^-------------';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.deleteShabondi.TRIGGER,
        payload: {
          params: { ...shabondiEntity, id },
          options: { paperApi },
        },
      },
    });
    const output$ = deleteShabondiEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteShabondi.REQUEST,
        payload: {
          shabondiId,
        },
      },
      u: {
        type: actions.setSelectedCell.TRIGGER,
        payload: null,
      },
      v: {
        type: actions.deleteShabondi.SUCCESS,
        payload: {
          shabondiId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(paperApi.updateElement).toHaveBeenCalledTimes(1);
    expect(paperApi.removeElement).toHaveBeenCalledTimes(1);
    expect(paperApi.updateElement).toHaveBeenCalledWith(id, {
      status: CELL_STATUS.pending,
    });
    expect(paperApi.removeElement).toHaveBeenCalledWith(id);
  });
});

it('should handle error properly', () => {
  const error = {
    data: {},
    meta: undefined,
    title: `Try to remove shabondi: "${shabondiEntity.name}" failed after retry 5 times.`,
  };
  const spyCreate = jest
    .spyOn(shabondiApi, 'remove')
    .mockReturnValue(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a------------|';
    const expected = '--a 9999ms (eu|)';
    const subs = '    ^--------------!';
    const id = '1234';

    const action$ = hot(input, {
      a: {
        type: actions.deleteShabondi.TRIGGER,
        payload: {
          params: { ...shabondiEntity, id },
          options: { paperApi },
        },
      },
    });
    const output$ = deleteShabondiEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteShabondi.REQUEST,
        payload: { shabondiId },
      },
      e: {
        type: actions.deleteShabondi.FAILURE,
        payload: { ...error, shabondiId },
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          ...error,
          shabondiId,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyCreate).toHaveBeenCalled();
    expect(paperApi.updateElement).toHaveBeenCalledTimes(2);
    expect(paperApi.updateElement).toHaveBeenCalledWith(id, {
      status: CELL_STATUS.pending,
    });
    expect(paperApi.updateElement).toHaveBeenCalledWith(id, {
      status: CELL_STATUS.failed,
    });
    expect(paperApi.removeElement).not.toHaveBeenCalled();
  });
});
