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
import * as volumeApi from 'api/volumeApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as volumeEntity } from 'api/__mocks__/volumeApi';
import deleteVolumeEpic from 'store/epics/volume/deleteVolumeEpic';

jest.mock('api/volumeApi');

const volumeId = getId(volumeEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('should delete a volume', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms v';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteVolume.TRIGGER,
        payload: { values: volumeEntity },
      },
    });
    const output$ = deleteVolumeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteVolume.REQUEST,
        payload: {
          volumeId,
        },
      },
      v: {
        type: actions.deleteVolume.SUCCESS,
        payload: {
          volumeId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('should delete multiple volumes', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---b           ';
    const expected = '--a---b 95ms v---y';
    const subs = '    ^-----------------';
    const anotherVolumeEntity = {
      values: { ...volumeEntity, name: 'volume2' },
    };

    const action$ = hot(input, {
      a: {
        type: actions.deleteVolume.TRIGGER,
        payload: { values: volumeEntity },
      },
      b: {
        type: actions.deleteVolume.TRIGGER,
        payload: anotherVolumeEntity,
      },
    });
    const output$ = deleteVolumeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteVolume.REQUEST,
        payload: {
          volumeId,
        },
      },
      v: {
        type: actions.deleteVolume.SUCCESS,
        payload: {
          volumeId,
        },
      },
      b: {
        type: actions.deleteVolume.REQUEST,
        payload: {
          volumeId: getId(anotherVolumeEntity.values),
        },
      },
      y: {
        type: actions.deleteVolume.SUCCESS,
        payload: {
          volumeId: getId(anotherVolumeEntity.values),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('delete same volume within period should be created once only', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-aa 10s a';
    const expected = '--a 99ms v';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.deleteVolume.TRIGGER,
        payload: { values: volumeEntity },
      },
    });
    const output$ = deleteVolumeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteVolume.REQUEST,
        payload: {
          volumeId,
        },
      },
      v: {
        type: actions.deleteVolume.SUCCESS,
        payload: {
          volumeId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of delete volume should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock delete volume failed',
  };
  const spyCreate = jest
    .spyOn(volumeApi, 'remove')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.deleteVolume.TRIGGER,
        payload: { values: volumeEntity },
      },
    });
    const output$ = deleteVolumeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteVolume.REQUEST,
        payload: { volumeId },
      },
      e: {
        type: actions.deleteVolume.FAILURE,
        payload: { ...error, volumeId },
      },
      u: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          ...error,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyCreate).toHaveBeenCalled();
  });
});
