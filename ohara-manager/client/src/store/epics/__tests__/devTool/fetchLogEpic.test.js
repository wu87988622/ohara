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

import { set } from 'lodash';
import { TestScheduler } from 'rxjs/testing';
import { StateObservable } from 'redux-observable';

import fetchLogEpic from '../../devTool/fetchLogEpic';
import * as logApi from 'api/logApi';
import * as actions from 'store/actions';
import { LOG_TIME_GROUP, KIND, GROUP, LOG_LEVEL } from 'const';
import { makeLog } from 'api/__mocks__/logApi';
import { throwError } from 'rxjs';

jest.mock('api/logApi');

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

beforeEach(() => {
  jest.restoreAllMocks();
});

const stateValues = {
  ui: {
    devTool: {
      log: {
        query: {
          logType: '',
          hostName: '',
          shabondiKey: { name: 'shd1', group: 'default' },
          streamKey: { name: 'app1', group: 'default' },
          timeGroup: LOG_TIME_GROUP.latest,
          timeRange: 10,
          startTime: '2020-04-15',
          endTime: '2020-04-14',
        },
      },
    },
    workspace: {
      name: 'workspace1',
    },
  },
};

it('fetch configurator log should be executed correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const state$ = new StateObservable(
      hot('v', {
        v: set(stateValues, 'ui.devTool.log.query.logType', KIND.configurator),
      }),
    );

    const logs = makeLog().split('\n');
    const input = '   ^-a----------|';
    const expected = '--- 99ms (ab|)';
    const subs = '    ^------------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchDevToolLog.TRIGGER,
        payload: {},
      },
    });
    const output$ = fetchLogEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchDevToolLog.SUCCESS,
        payload: [
          {
            logs,
            name: 'node01',
          },
          {
            logs,
            name: 'node02',
          },
        ],
      },
      b: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          hostName: 'node01',
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch configurator log multiple times should be executed the first one until finished', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;
    const spyGetLog = jest.spyOn(logApi, 'getConfiguratorLog');

    const state$ = new StateObservable(
      hot('v', {
        v: set(stateValues, 'ui.devTool.log.query.logType', KIND.configurator),
      }),
    );

    const logs = makeLog().split('\n');
    const input = '   ^-a-a-a  98ms a--------|';
    const expected = '--- 99ms (ab) 99ms (ab|)';
    const subs = '    ^------  98ms ---------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchDevToolLog.TRIGGER,
        payload: {},
      },
    });
    const output$ = fetchLogEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchDevToolLog.SUCCESS,
        payload: [
          {
            logs,
            name: 'node01',
          },
          {
            logs,
            name: 'node02',
          },
        ],
      },
      b: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          hostName: 'node01',
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();

    expect(spyGetLog).toHaveBeenCalledTimes(2);
  });
});

it('fetch zookeeper log should be executed correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const state$ = new StateObservable(
      hot('v', {
        v: set(stateValues, 'ui.devTool.log.query.logType', KIND.zookeeper),
      }),
    );

    const logs = makeLog({
      name: stateValues.ui.workspace.name,
      group: GROUP.ZOOKEEPER,
    }).split('\n');
    const input = '   ^-a----------|';
    const expected = '--- 99ms (ab|)';
    const subs = '    ^------------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchDevToolLog.TRIGGER,
        payload: {},
      },
    });
    const output$ = fetchLogEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchDevToolLog.SUCCESS,
        payload: [
          {
            logs,
            name: 'node01',
          },
          {
            logs,
            name: 'node02',
          },
        ],
      },
      b: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          hostName: 'node01',
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch zookeeper log multiple times should be executed the first one until finished', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const state$ = new StateObservable(
      hot('v', {
        v: set(stateValues, 'ui.devTool.log.query.logType', KIND.zookeeper),
      }),
    );

    const logs = makeLog({
      name: stateValues.ui.workspace.name,
      group: GROUP.ZOOKEEPER,
    }).split('\n');
    const input = '   ^-a-a-a  98ms a--------|';
    const expected = '--- 99ms (ab) 99ms (ab|)';
    const subs = '    ^------  98ms ---------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchDevToolLog.TRIGGER,
        payload: {},
      },
    });
    const output$ = fetchLogEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchDevToolLog.SUCCESS,
        payload: [
          {
            logs,
            name: 'node01',
          },
          {
            logs,
            name: 'node02',
          },
        ],
      },
      b: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          hostName: 'node01',
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch broker log should be executed correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const state$ = new StateObservable(
      hot('v', {
        v: set(stateValues, 'ui.devTool.log.query.logType', KIND.broker),
      }),
    );

    const logs = makeLog({
      name: stateValues.ui.workspace.name,
      group: GROUP.BROKER,
    }).split('\n');
    const input = '   ^-a----------|';
    const expected = '--- 99ms (ab|)';
    const subs = '    ^------------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchDevToolLog.TRIGGER,
        payload: {},
      },
    });
    const output$ = fetchLogEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchDevToolLog.SUCCESS,
        payload: [
          {
            logs,
            name: 'node01',
          },
          {
            logs,
            name: 'node02',
          },
        ],
      },
      b: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          hostName: 'node01',
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch broker log multiple times should be executed the first one until finished', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const state$ = new StateObservable(
      hot('v', {
        v: set(stateValues, 'ui.devTool.log.query.logType', KIND.broker),
      }),
    );

    const logs = makeLog({
      name: stateValues.ui.workspace.name,
      group: GROUP.BROKER,
    }).split('\n');
    const input = '   ^-a-a-a  98ms a--------|';
    const expected = '--- 99ms (ab) 99ms (ab|)';
    const subs = '    ^------  98ms ---------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchDevToolLog.TRIGGER,
        payload: {},
      },
    });
    const output$ = fetchLogEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchDevToolLog.SUCCESS,
        payload: [
          {
            logs,
            name: 'node01',
          },
          {
            logs,
            name: 'node02',
          },
        ],
      },
      b: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          hostName: 'node01',
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch worker log should be executed correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const state$ = new StateObservable(
      hot('v', {
        v: set(stateValues, 'ui.devTool.log.query.logType', KIND.worker),
      }),
    );

    const logs = makeLog({
      name: stateValues.ui.workspace.name,
      group: GROUP.WORKER,
    }).split('\n');
    const input = '   ^-a----------|';
    const expected = '--- 99ms (ab|)';
    const subs = '    ^------------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchDevToolLog.TRIGGER,
        payload: {},
      },
    });
    const output$ = fetchLogEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchDevToolLog.SUCCESS,
        payload: [
          {
            logs,
            name: 'node01',
          },
          {
            logs,
            name: 'node02',
          },
        ],
      },
      b: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          hostName: 'node01',
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch worker log multiple times should be executed the first one until finished', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const state$ = new StateObservable(
      hot('v', {
        v: set(stateValues, 'ui.devTool.log.query.logType', KIND.worker),
      }),
    );

    const logs = makeLog({
      name: stateValues.ui.workspace.name,
      group: GROUP.WORKER,
    }).split('\n');
    const input = '   ^-a-a-a  98ms a--------|';
    const expected = '--- 99ms (ab) 99ms (ab|)';
    const subs = '    ^------  98ms ---------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchDevToolLog.TRIGGER,
        payload: {},
      },
    });
    const output$ = fetchLogEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchDevToolLog.SUCCESS,
        payload: [
          {
            logs,
            name: 'node01',
          },
          {
            logs,
            name: 'node02',
          },
        ],
      },
      b: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          hostName: 'node01',
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch shabondi log should be executed correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const state$ = new StateObservable(
      hot('v', {
        v: set(stateValues, 'ui.devTool.log.query.logType', KIND.shabondi),
      }),
    );

    const logs = makeLog(stateValues.ui.devTool.log.query.shabondiKey).split(
      '\n',
    );
    const input = '   ^-a----------|';
    const expected = '--- 99ms (ab|)';
    const subs = '    ^------------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchDevToolLog.TRIGGER,
        payload: {},
      },
    });
    const output$ = fetchLogEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchDevToolLog.SUCCESS,
        payload: [
          {
            logs,
            name: 'node01',
          },
          {
            logs,
            name: 'node02',
          },
        ],
      },
      b: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          hostName: 'node01',
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch stream log should be executed correctly', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const state$ = new StateObservable(
      hot('v', {
        v: set(stateValues, 'ui.devTool.log.query.logType', KIND.stream),
      }),
    );

    const logs = makeLog(stateValues.ui.devTool.log.query.streamKey).split(
      '\n',
    );
    const input = '   ^-a----------|';
    const expected = '--- 99ms (ab|)';
    const subs = '    ^------------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchDevToolLog.TRIGGER,
        payload: {},
      },
    });
    const output$ = fetchLogEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchDevToolLog.SUCCESS,
        payload: [
          {
            logs,
            name: 'node01',
          },
          {
            logs,
            name: 'node02',
          },
        ],
      },
      b: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          hostName: 'node01',
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('fetch stream log multiple times should be executed the first one until finished', () => {
  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const state$ = new StateObservable(
      hot('v', {
        v: set(stateValues, 'ui.devTool.log.query.logType', KIND.stream),
      }),
    );

    const logs = makeLog(stateValues.ui.devTool.log.query.streamKey).split(
      '\n',
    );
    const input = '   ^-a-a-a  98ms a--------|';
    const expected = '--- 99ms (ab) 99ms (ab|)';
    const subs = '    ^------  98ms ---------!';

    const action$ = hot(input, {
      a: {
        type: actions.fetchDevToolLog.TRIGGER,
        payload: {},
      },
    });
    const output$ = fetchLogEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.fetchDevToolLog.SUCCESS,
        payload: [
          {
            logs,
            name: 'node01',
          },
          {
            logs,
            name: 'node02',
          },
        ],
      },
      b: {
        type: actions.setDevToolLogQueryParams.SUCCESS,
        payload: {
          hostName: 'node01',
        },
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});

it('throw exception of fetch log should also trigger event log action', () => {
  const error = {
    status: -1,
    data: {},
    title: 'mock fetch log failed',
  };
  const spyCreate = jest
    .spyOn(logApi, 'getConfiguratorLog')
    .mockReturnValueOnce(throwError(error));

  makeTestScheduler().run((helpers) => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a-----|';
    const expected = '--(eu)--|';
    const subs = '    ^-------!';

    const state$ = new StateObservable(
      hot('v', {
        v: set(stateValues, 'ui.devTool.log.query.logType', KIND.configurator),
      }),
    );

    const action$ = hot(input, {
      a: {
        type: actions.fetchDevToolLog.TRIGGER,
      },
    });
    const output$ = fetchLogEpic(action$, state$);

    expectObservable(output$).toBe(expected, {
      e: {
        type: actions.fetchDevToolLog.FAILURE,
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
