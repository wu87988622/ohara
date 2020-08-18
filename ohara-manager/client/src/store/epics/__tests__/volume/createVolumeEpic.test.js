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
import createVolumeEpic from 'store/epics/volume/createVolumeEpic';

jest.mock('api/volumeApi');

const volumeId = getId(volumeEntity);
const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('should create a volume', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a         ';
    const expected = '--a 99ms u';
    const subs = '    ^-----------';

    const action$ = hot(input, {
      a: {
        type: actions.createVolume.TRIGGER,
        payload: volumeEntity,
      },
    });
    const output$ = createVolumeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createVolume.REQUEST,
        payload: {
          volumeId,
        },
      },
      u: {
        type: actions.createVolume.SUCCESS,
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
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('should create multiple volumes', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-ab          ';
    const expected = '--ab 98ms uv';
    const subs = '    ^-------------';
    const anotherVolumeEntity = { ...volumeEntity, name: 'volume2' };

    const action$ = hot(input, {
      a: {
        type: actions.createVolume.TRIGGER,
        payload: volumeEntity,
      },
      b: {
        type: actions.createVolume.TRIGGER,
        payload: anotherVolumeEntity,
      },
    });
    const output$ = createVolumeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createVolume.REQUEST,
        payload: {
          volumeId,
        },
      },
      u: {
        type: actions.createVolume.SUCCESS,
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
      b: {
        type: actions.createVolume.REQUEST,
        payload: {
          volumeId: getId(anotherVolumeEntity),
        },
      },
      v: {
        type: actions.createVolume.SUCCESS,
        payload: {
          volumeId: getId(anotherVolumeEntity),
          entities: {
            volumes: {
              [getId(anotherVolumeEntity)]: anotherVolumeEntity,
            },
          },
          result: getId(anotherVolumeEntity),
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('create same volume within period should be created once only', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-aa 10s a  ';
    const expected = '--a 99ms u';
    const subs = '    ^-----------';

    const action$ = hot(input, {
      a: {
        type: actions.createVolume.TRIGGER,
        payload: volumeEntity,
      },
    });
    const output$ = createVolumeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createVolume.REQUEST,
        payload: {
          volumeId,
        },
      },
      u: {
        type: actions.createVolume.SUCCESS,
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
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of create volume should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock create volume failed',
  };
  const spyCreate = jest
    .spyOn(volumeApi, 'create')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.createVolume.TRIGGER,
        payload: volumeEntity,
      },
    });
    const output$ = createVolumeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.createVolume.REQUEST,
        payload: { volumeId },
      },
      e: {
        type: actions.createVolume.FAILURE,
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
