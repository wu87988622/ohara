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

import setDevToolLogQueryParamsEpic from '../../devTool/setDevToolLogQueryParamsEpic';
import * as actions from 'store/actions';
import { LOG_TIME_GROUP, KIND } from 'const';

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

const values = {
  params: {
    logType: '',
    hostName: 'node01',
    streamKey: {},
    timeGroup: LOG_TIME_GROUP.customize,
    timeRange: 10,
    startTime: '2020-04-15',
    endTime: '2020-04-16',
  },
};

it('set devTool log query params should be executed correctly', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a--------b-----c------d----|';
    const expected = '--(abcde)--(pqr)-(tuv)--(xy)-|';
    const subs = '    ^----------------------------!';

    const action$ = hot(input, {
      a: {
        type: actions.setDevToolLogQueryParams.TRIGGER,
        payload: values,
      },
      b: {
        type: actions.setDevToolLogQueryParams.TRIGGER,
        payload: {
          params: { streamName: 'app' },
          streamGroup: 'group1',
        },
      },
      c: {
        type: actions.setDevToolLogQueryParams.TRIGGER,
        payload: {
          params: { logType: KIND.zookeeper },
        },
      },
      d: {
        type: actions.setDevToolLogQueryParams.TRIGGER,
        payload: {
          params: { logType: KIND.stream },
        },
      },
    });
    const output$ = setDevToolLogQueryParamsEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          hostName: 'node01',
        },
      },
      b: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          timeGroup: LOG_TIME_GROUP.customize,
        },
      },
      c: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          timeRange: 10,
        },
      },
      d: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          startTime: '2020-04-15',
        },
      },
      e: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          endTime: '2020-04-16',
        },
      },
      p: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          hostName: '',
        },
      },
      q: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          streamKey: { name: 'app', group: 'group1' },
        },
      },
      r: {
        type: actions.fetchDevToolLog.TRIGGER,
      },
      t: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          logType: KIND.zookeeper,
        },
      },
      u: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          hostName: '',
        },
      },
      v: {
        type: actions.fetchDevToolLog.TRIGGER,
      },
      x: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          logType: KIND.stream,
        },
      },
      y: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          hostName: '',
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
