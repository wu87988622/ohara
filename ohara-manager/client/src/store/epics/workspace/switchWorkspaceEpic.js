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

import { normalize } from 'normalizr';
import { isEmpty, merge } from 'lodash';
import { ofType } from 'redux-observable';
import { of, forkJoin, interval } from 'rxjs';
import {
  catchError,
  debounce,
  filter,
  map,
  switchMap,
  startWith,
  withLatestFrom,
} from 'rxjs/operators';

import * as brokerApi from 'api/brokerApi';
import * as inspectApi from 'api/inspectApi';
import * as workerApi from 'api/workerApi';
import * as zookeeperApi from 'api/zookeeperApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import * as selectors from 'store/selectors';

const workerGroup = 'worker';
const brokerGroup = 'broker';
const zookeeperGroup = 'zookeeper';

const fetchWorkerObservable = (name, group = workerGroup) =>
  of(name).pipe(
    switchMap(name => workerApi.get({ group, name })),
    map(res => res.data),
    map(data => normalize(data, schema.worker)),
  );

const fetchWorkerInfoObservable = (name, group = workerGroup) =>
  of(name).pipe(
    switchMap(name => inspectApi.getWorkerInfo({ group, name })),
    map(res => ({ ...res.data, group, name })),
    map(data => normalize(data, schema.info)),
  );

const fetchBrokerObservable = (name, group = brokerGroup) =>
  of(name).pipe(
    switchMap(name => brokerApi.get({ group, name })),
    map(res => res.data),
    map(data => normalize(data, schema.broker)),
  );

const fetchBrokerInfoObservable = (name, group = brokerGroup) =>
  of(name).pipe(
    switchMap(name => inspectApi.getBrokerInfo({ group, name })),
    map(res => ({ ...res.data, group, name })),
    map(data => normalize(data, schema.info)),
  );

const fetchZookeeperObservable = (name, group = zookeeperGroup) =>
  of(name).pipe(
    switchMap(name => zookeeperApi.get({ group, name })),
    map(res => res.data),
    map(data => normalize(data, schema.zookeeper)),
  );

const fetchZookeeperInfoObservable = (name, group = zookeeperGroup) =>
  of(name).pipe(
    switchMap(name => inspectApi.getZookeeperInfo({ group, name })),
    map(res => ({ ...res.data, group, name })),
    map(data => normalize(data, schema.info)),
  );

export default (action$, state$) =>
  action$.pipe(
    ofType(actions.switchWorkspace.TRIGGER),
    filter(action => !isEmpty(action.payload)),
    debounce(() => interval(1000)),
    withLatestFrom(state$),
    switchMap(([action, state]) => {
      const { name } = action.payload;
      const getWorkerByName = selectors.makeGetWorkerByName();
      const worker = getWorkerByName(state, { name });
      if (worker) return of(actions.switchWorkspace.success({ name }));
      return forkJoin(
        fetchWorkerObservable(name),
        fetchWorkerInfoObservable(name),
        fetchBrokerObservable(name),
        fetchBrokerInfoObservable(name),
        fetchZookeeperObservable(name),
        fetchZookeeperInfoObservable(name),
      ).pipe(
        map(entitiesAry => merge(...entitiesAry, { name })),
        map(nameAndEntities =>
          actions.switchWorkspace.success(nameAndEntities),
        ),
        startWith(actions.switchWorkspace.request()),
        catchError(res => of(actions.fetchWorker.failure(res))),
      );
    }),
  );
