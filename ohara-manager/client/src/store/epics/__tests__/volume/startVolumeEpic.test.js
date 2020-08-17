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

import { omit } from 'lodash';
import { of } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import { LOG_LEVEL } from 'const';
import * as volumeApi from 'api/volumeApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as volumeEntity } from 'api/__mocks__/volumeApi';
import startVolumeEpic from 'store/epics/volume/startVolumeEpic';

jest.mock('api/volumeApi');

const volumeId = getId(volumeEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  // ensure the mock data is as expected before each test
  jest.restoreAllMocks();
});

it('start topic should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 199ms v';
    const subs = '    ^----------';

    const action$ = hot(input, {
      a: {
        type: actions.startVolume.TRIGGER,
        payload: volumeEntity,
      },
    });
    const output$ = startVolumeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startVolume.REQUEST,
        payload: {
          volumeId,
        },
      },
      v: {
        type: actions.startVolume.SUCCESS,
        payload: {
          volumeId,
          entities: {
            volumes: {
              [volumeId]: {
                ...volumeEntity,
                state: 'RUNNING',
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

it('start volume failed after reach retry limit', () => {
  // mock a 20 times "failed started" result
  const spyGet = jest.spyOn(volumeApi, 'get');
  for (let i = 0; i < 20; i++) {
    spyGet.mockReturnValueOnce(
      of({
        status: 200,
        title: 'retry mock get data',
        data: { ...omit(volumeEntity, 'state') },
      }),
    );
  }
  // get result finally
  spyGet.mockReturnValueOnce(
    of({
      status: 200,
      title: 'retry mock get data',
      data: { ...volumeEntity, state: 'RUNNING' },
    }),
  );

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a             ';
    // we failed after retry 11 times (11 * 2000ms = 22s)
    const expected = '--a 31099ms (vz)';
    const subs = '    ^---------------';

    const action$ = hot(input, {
      a: {
        type: actions.startVolume.TRIGGER,
        payload: volumeEntity,
      },
    });
    const output$ = startVolumeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startVolume.REQUEST,
        payload: {
          volumeId,
        },
      },
      v: {
        type: actions.startVolume.FAILURE,
        payload: {
          volumeId,
          data: volumeEntity,
          status: 200,
          title: `Failed to start volume ${volumeEntity.name}: Unable to confirm the status of the volume is running`,
        },
      },
      z: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          data: volumeEntity,
          status: 200,
          title: `Failed to start volume ${volumeEntity.name}: Unable to confirm the status of the volume is running`,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('start volume multiple times should be worked once', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---a 1s a 10s ';
    const expected = '--a       199ms v';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.startVolume.TRIGGER,
        payload: volumeEntity,
      },
    });
    const output$ = startVolumeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.startVolume.REQUEST,
        payload: { volumeId },
      },
      v: {
        type: actions.startVolume.SUCCESS,
        payload: {
          volumeId,
          entities: {
            volumes: {
              [volumeId]: {
                ...volumeEntity,
                state: 'RUNNING',
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
