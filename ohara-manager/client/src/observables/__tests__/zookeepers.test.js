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

import { of, throwError } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';
import { times } from 'lodash';

import * as zookeeperApi from 'api/zookeeperApi';
import { entity as zookeeperEntity } from 'api/__mocks__/zookeeperApi';
import { fetchZookeeper, startZookeeper } from '../zookeepers';

jest.mock('api/zookeeperApi');

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

const RESPONSES = {
  success: {
    status: 200,
    title: 'retry mock get data',
    data: zookeeperEntity,
  },
  successWithRunning: {
    status: 200,
    title: 'retry mock get data',
    data: { ...zookeeperEntity, state: 'RUNNING' },
  },
  error: { status: -1, data: {}, title: 'mock get zookeeper failed' },
};

it('get zookeeper should be worked correctly', () => {
  makeTestScheduler().run(({ expectObservable }) => {
    const params = { group: zookeeperEntity.group, name: zookeeperEntity.name };

    const expected = '- 499ms (v|)';

    const output$ = fetchZookeeper(params);

    expectObservable(output$).toBe(expected, {
      v: zookeeperEntity,
    });
  });
});

it('get zookeeper should be failed - Retry limit reached', () => {
  makeTestScheduler().run(({ expectObservable }) => {
    jest.restoreAllMocks();
    const spyGet = jest.spyOn(zookeeperApi, 'get');
    times(20, () => spyGet.mockReturnValueOnce(throwError(RESPONSES.error)));

    const params = { group: zookeeperEntity.group, name: zookeeperEntity.name };

    const expected = '#';

    const output$ = fetchZookeeper(params);

    expectObservable(output$).toBe(expected, null, RESPONSES.error);
  });
});

it('start zookeeper should run in two minutes', () => {
  makeTestScheduler().run(({ expectObservable, flush }) => {
    jest.restoreAllMocks();
    const spyStart = jest.spyOn(zookeeperApi, 'start');
    const spyGet = jest.spyOn(zookeeperApi, 'get');
    times(5, () => spyGet.mockReturnValueOnce(of(RESPONSES.success)));
    spyGet.mockReturnValueOnce(of(RESPONSES.successWithRunning));

    const params = {
      group: zookeeperEntity.group,
      name: zookeeperEntity.name,
    };

    const expected = '10s 10ms (v|)';

    const output$ = startZookeeper(params, true);

    expectObservable(output$).toBe(expected, {
      v: RESPONSES.successWithRunning.data,
    });

    flush();

    expect(spyStart).toHaveBeenCalled();
    expect(spyGet).toHaveBeenCalled();

    // The mock function is called six times
    expect(spyStart.mock.calls.length).toBe(6);
    expect(spyGet.mock.calls.length).toBe(6);
  });
});
