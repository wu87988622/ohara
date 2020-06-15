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
import { delay } from 'rxjs/operators';
import { TestScheduler } from 'rxjs/testing';
import { times } from 'lodash';

import * as workerApi from 'api/workerApi';
import { entity as workerEntity } from 'api/__mocks__/workerApi';
import { getWorker, checkWorkerRunning, startWorker } from '../workers';

jest.mock('api/workerApi');

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

const RESPONSES = {
  success: {
    status: 200,
    title: 'retry mock get data',
    data: workerEntity,
  },
  successWithRunning: {
    status: 200,
    title: 'retry mock get data',
    data: { ...workerEntity, state: 'RUNNING' },
  },
  error: { status: -1, data: {}, title: 'mock get worker failed' },
};

it('get worker should be worked correctly', () => {
  makeTestScheduler().run(({ expectObservable }) => {
    const params = { group: workerEntity.group, name: workerEntity.name };

    const expected = '- 499ms (v|)';

    const output$ = getWorker(params);

    expectObservable(output$).toBe(expected, {
      v: workerEntity,
    });
  });
});

it('get worker should be worked correctly - Retry', () => {
  makeTestScheduler().run(({ expectObservable }) => {
    jest.restoreAllMocks();
    const spyGet = jest.spyOn(workerApi, 'get');
    times(3, () => spyGet.mockReturnValueOnce(throwError(RESPONSES.error)));
    spyGet.mockReturnValueOnce(of(RESPONSES.success).pipe(delay(500)));

    const params = { group: workerEntity.group, name: workerEntity.name };

    const expected = '500ms (v|)';

    const output$ = getWorker(params);

    expectObservable(output$).toBe(expected, {
      v: workerEntity,
    });
  });
});

it('get worker should be failed - Retry limit reached', () => {
  makeTestScheduler().run(({ expectObservable }) => {
    jest.restoreAllMocks();
    const spyGet = jest.spyOn(workerApi, 'get');
    times(20, () => spyGet.mockReturnValueOnce(throwError(RESPONSES.error)));

    const params = { group: workerEntity.group, name: workerEntity.name };

    const expected = '#';

    const output$ = getWorker(params);

    expectObservable(output$).toBe(expected, null, RESPONSES.error);
  });
});

it('check worker should be running', () => {
  makeTestScheduler().run(({ expectObservable }) => {
    jest.restoreAllMocks();
    const spyGet = jest.spyOn(workerApi, 'get');
    spyGet.mockReturnValueOnce(of(RESPONSES.successWithRunning));

    const params = { group: workerEntity.group, name: workerEntity.name };

    const expected = '(v|)';

    const output$ = checkWorkerRunning(params);

    expectObservable(output$).toBe(expected, {
      v: true,
    });
  });
});

it('start worker should run in two minutes', () => {
  makeTestScheduler().run(({ expectObservable, flush }) => {
    jest.restoreAllMocks();
    const spyStart = jest.spyOn(workerApi, 'start');
    const spyGet = jest.spyOn(workerApi, 'get');
    times(5, () => spyGet.mockReturnValueOnce(of(RESPONSES.success)));
    spyGet.mockReturnValueOnce(of(RESPONSES.successWithRunning));

    const params = { group: workerEntity.group, name: workerEntity.name };

    const expected = '10s (v|)';

    const output$ = startWorker(params, true);

    expectObservable(output$).toBe(expected, { v: true });

    flush();

    expect(spyStart).toHaveBeenCalled();
    expect(spyGet).toHaveBeenCalled();
  });
});
