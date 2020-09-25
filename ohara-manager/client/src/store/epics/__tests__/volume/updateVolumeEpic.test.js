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

import * as actions from 'store/actions';
import * as volumeApi from 'api/volumeApi';
import { getId } from 'utils/object';
import { entity as volumeEntity } from 'api/__mocks__/volumeApi';
import updateVolumeEpic from 'store/epics/volume/updateVolumeEpic';
import { throwError } from 'rxjs';
import { LOG_LEVEL } from 'const';

jest.mock('api/volumeApi');

const volumeId = getId(volumeEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('update volume should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.updateVolume.TRIGGER,
        payload: { values: { ...volumeEntity, nodeNames: ['node01'] } },
      },
    });
    const output$ = updateVolumeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateVolume.REQUEST,
        payload: {
          volumeId,
        },
      },
      u: {
        type: actions.updateVolume.SUCCESS,
        payload: {
          volumeId,
          entities: {
            volumes: {
              [volumeId]: { ...volumeEntity, nodeNames: ['node01'] },
            },
          },
          result: volumeId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('update volume multiple times should got latest result', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 60ms c 10s            ';
    const expected = '--a-b 60ms d 36ms u-v 60ms w';
    const subs = '    ^---------------------------';

    const action$ = hot(input, {
      a: {
        type: actions.updateVolume.TRIGGER,
        payload: { values: { ...volumeEntity } },
      },
      b: {
        type: actions.updateVolume.TRIGGER,
        payload: { values: { ...volumeEntity, nodeNames: ['node01'] } },
      },
      c: {
        type: actions.updateVolume.TRIGGER,
        payload: { values: { ...volumeEntity, nodeNames: ['node02'] } },
      },
    });
    const output$ = updateVolumeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateVolume.REQUEST,
        payload: {
          volumeId,
        },
      },
      b: {
        type: actions.updateVolume.REQUEST,
        payload: {
          volumeId,
        },
      },
      d: {
        type: actions.updateVolume.REQUEST,
        payload: {
          volumeId,
        },
      },
      u: {
        type: actions.updateVolume.SUCCESS,
        payload: {
          volumeId,
          entities: {
            volumes: {
              [volumeId]: volumeEntity,
            },
          },
          result: volumeId,
        },
      },
      v: {
        type: actions.updateVolume.SUCCESS,
        payload: {
          volumeId,
          entities: {
            volumes: {
              [volumeId]: {
                ...volumeEntity,
                nodeNames: ['node01'],
              },
            },
          },
          result: volumeId,
        },
      },
      w: {
        type: actions.updateVolume.SUCCESS,
        payload: {
          volumeId,
          entities: {
            volumes: {
              [volumeId]: {
                ...volumeEntity,
                nodeNames: ['node02'],
              },
            },
          },
          result: volumeId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of update volume should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock update volume failed',
  };
  const spyCreate = jest
    .spyOn(volumeApi, 'update')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.updateVolume.TRIGGER,
        payload: { values: { ...volumeEntity } },
      },
    });
    const output$ = updateVolumeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.updateVolume.REQUEST,
        payload: { volumeId },
      },
      e: {
        type: actions.updateVolume.FAILURE,
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
