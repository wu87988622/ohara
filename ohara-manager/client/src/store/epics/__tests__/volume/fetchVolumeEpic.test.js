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
import { ENTITY_TYPE } from 'store/schema';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as volumeEntity } from 'api/__mocks__/volumeApi';
import fetchVolumesEpic from 'store/epics/volume/fetchVolumesEpic';

jest.mock('api/volumeApi');

const volumeId = getId(volumeEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('fetch volumes should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a----------|';
    const expected = '--a 99ms (u|)';
    const subs = '    ^------------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchVolumes.TRIGGER,
      },
    });
    const output$ = fetchVolumesEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchVolumes.REQUEST,
      },
      u: {
        type: actions.fetchVolumes.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.volumes]: {
              [volumeId]: volumeEntity,
            },
          },
          result: [volumeId],
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch volume multiple times within period should be got latest result', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a--b----------|';
    const expected = '--a--b 99ms (u|)';
    const subs = '    ^---------------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchVolumes.TRIGGER,
      },
      b: {
        type: actions.fetchVolumes.TRIGGER,
      },
    });
    const output$ = fetchVolumesEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchVolumes.REQUEST,
      },
      b: {
        type: actions.fetchVolumes.REQUEST,
      },
      u: {
        type: actions.fetchVolumes.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.volumes]: {
              [volumeId]: volumeEntity,
            },
          },
          result: [volumeId],
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of fetch volume list should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock get volume list failed',
  };
  const spyCreate = jest
    .spyOn(volumeApi, 'getAll')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchVolumes.TRIGGER,
      },
    });
    const output$ = fetchVolumesEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchVolumes.REQUEST,
      },
      e: {
        type: actions.fetchVolumes.FAILURE,
        payload: { ...error },
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
