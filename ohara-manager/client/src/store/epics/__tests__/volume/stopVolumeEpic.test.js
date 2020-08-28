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
import { of } from 'rxjs';

import stopVolumeEpic from '../../volume/stopVolumeEpic';
import * as volumeApi from 'api/volumeApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as volumeEntity } from 'api/__mocks__/volumeApi';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import { LOG_LEVEL } from 'const';

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

it('stop volume should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a        ';
    const expected = '--a 199ms v';
    const subs = '    ^----------';

    const action$ = hot(input, {
      a: {
        type: actions.stopVolume.TRIGGER,
        payload: { values: volumeEntity },
      },
    });
    const output$ = stopVolumeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopVolume.REQUEST,
        payload: {
          volumeId,
        },
      },
      v: {
        type: actions.stopVolume.SUCCESS,
        payload: {
          volumeId,
          entities: {
            volumes: {
              [volumeId]: {
                ...volumeEntity,
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

it('stop topic failed after reach retry limit', () => {
  // mock a 20 times "failed stoped" result
  const spyGet = jest.spyOn(volumeApi, 'get');
  for (let i = 0; i < 20; i++) {
    spyGet.mockReturnValueOnce(
      of({
        status: 200,
        title: 'retry mock get data',
        data: { ...volumeEntity, state: SERVICE_STATE.RUNNING },
      }),
    );
  }
  // get result finally
  spyGet.mockReturnValueOnce(
    of({
      status: 200,
      title: 'retry mock get data',
      data: { ...volumeEntity },
    }),
  );

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a             ';
    // we failed after retry 11 times (11 * 2000ms = 22s)
    const expected = '--a 31099ms (vy)';
    const subs = '    ^---------------';

    const action$ = hot(input, {
      a: {
        type: actions.stopVolume.TRIGGER,
        payload: { values: volumeEntity },
      },
    });
    const output$ = stopVolumeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopVolume.REQUEST,
        payload: {
          volumeId,
        },
      },
      v: {
        type: actions.stopVolume.FAILURE,
        payload: {
          volumeId,
          data: { ...volumeEntity, state: SERVICE_STATE.RUNNING },
          status: 200,
          title: `Failed to stop volume ${volumeEntity.name}: Unable to confirm the status of the volume is not running`,
        },
      },
      y: {
        type: actions.createEventLog.TRIGGER,
        payload: {
          data: { ...volumeEntity, state: SERVICE_STATE.RUNNING },
          status: 200,
          title: `Failed to stop volume ${volumeEntity.name}: Unable to confirm the status of the volume is not running`,
          type: LOG_LEVEL.error,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('stop volume multiple times should be worked once', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a---a 1s a 10s ';
    const expected = '--a       199ms v';
    const subs = '    ^----------------';

    const action$ = hot(input, {
      a: {
        type: actions.stopVolume.TRIGGER,
        payload: { values: volumeEntity },
      },
    });
    const output$ = stopVolumeEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.stopVolume.REQUEST,
        payload: { volumeId },
      },
      v: {
        type: actions.stopVolume.SUCCESS,
        payload: {
          volumeId,
          entities: {
            volumes: {
              [volumeId]: {
                ...volumeEntity,
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
