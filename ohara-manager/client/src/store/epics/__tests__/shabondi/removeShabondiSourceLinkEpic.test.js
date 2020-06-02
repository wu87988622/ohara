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
import * as shabondiApi from 'api/shabondiApi';
import removeShabondiSourceLinkEpic from '../../shabondi/removeShabondiSourceLinkEpic';
import { ENTITY_TYPE } from 'store/schema';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { entity as shabondiEntity } from 'api/__mocks__/shabondiApi';

jest.mock('api/shabondiApi');
const mockedPaperApi = jest.fn(() => {
  return {
    addLink: () => noop(),
  };
});
const paperApi = new mockedPaperApi();

const shabondiId = getId(shabondiEntity);

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('remove source link of shabondi should be worked correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a       ';
    const expected = '--a 99ms u';
    const subs = '    ^---------';

    const action$ = hot(input, {
      a: {
        type: actions.removeShabondiSourceLink.TRIGGER,
        payload: {
          params: { ...shabondiEntity, jmxPort: 999 },
          options: { paperApi },
        },
      },
    });
    const output$ = removeShabondiSourceLinkEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.removeShabondiSourceLink.REQUEST,
      },
      u: {
        type: actions.removeShabondiSourceLink.SUCCESS,
        payload: {
          entities: {
            [ENTITY_TYPE.shabondis]: {
              [shabondiId]: { ...shabondiEntity, jmxPort: 999 },
            },
          },
          result: shabondiId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('remove shabondi source link multiple times should got latest result', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-b 60ms c 10s        ';
    const expected = '--a-b 60ms d 39ms 60ms w';
    const subs = '    ^-----------------------';

    const action$ = hot(input, {
      a: {
        type: actions.removeShabondiSourceLink.TRIGGER,
        payload: {
          params: shabondiEntity,
          options: { paperApi },
        },
      },
      b: {
        type: actions.removeShabondiSourceLink.TRIGGER,
        payload: {
          params: { ...shabondiEntity, nodeNames: ['n1', 'n2'] },
          options: { paperApi },
        },
      },
      c: {
        type: actions.removeShabondiSourceLink.TRIGGER,
        payload: {
          params: { ...shabondiEntity, clientPort: 1234 },
          options: { paperApi },
        },
      },
    });
    const output$ = removeShabondiSourceLinkEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.removeShabondiSourceLink.REQUEST,
      },
      b: {
        type: actions.removeShabondiSourceLink.REQUEST,
      },
      d: {
        type: actions.removeShabondiSourceLink.REQUEST,
      },
      w: {
        type: actions.removeShabondiSourceLink.SUCCESS,
        payload: {
          entities: {
            shabondis: {
              [shabondiId]: {
                ...shabondiEntity,
                clientPort: 1234,
              },
            },
          },
          result: shabondiId,
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of remove shabondi source link should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock remove shabondi source link failed',
  };
  const spyCreate = jest
    .spyOn(shabondiApi, 'update')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(aeu)-|';
    const subs = '    ^-------!';

    const action$ = hot(input, {
      a: {
        type: actions.removeShabondiSourceLink.TRIGGER,
        payload: {
          params: shabondiEntity,
          options: { paperApi, topic: {} },
        },
      },
    });
    const output$ = removeShabondiSourceLinkEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.removeShabondiSourceLink.REQUEST,
      },
      e: {
        type: actions.removeShabondiSourceLink.FAILURE,
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
