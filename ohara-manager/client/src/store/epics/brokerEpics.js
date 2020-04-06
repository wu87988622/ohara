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
import { combineEpics, ofType } from 'redux-observable';
import { from, of } from 'rxjs';
import { switchMap, map, startWith, catchError } from 'rxjs/operators';

import * as brokerApi from 'api/brokerApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';

const fetchBrokerEpic = action$ =>
  action$.pipe(
    ofType(actions.fetchBroker.TRIGGER),
    map(action => action.payload),
    switchMap(values =>
      from(brokerApi.get(values)).pipe(
        map(res => normalize(res.data, schema.broker)),
        map(entities => actions.fetchBroker.success(entities)),
        startWith(actions.fetchBroker.request()),
        catchError(res => of(actions.fetchBroker.failure(res))),
      ),
    ),
  );

const createBrokerEpic = action$ =>
  action$.pipe(
    ofType(actions.createBroker.TRIGGER),
    map(action => action.payload),
    switchMap(values =>
      from(brokerApi.create(values)).pipe(
        map(res => normalize(res.data, schema.broker)),
        map(entities => actions.createBroker.success(entities)),
        startWith(actions.createBroker.request()),
        catchError(res => of(actions.createBroker.failure(res))),
      ),
    ),
  );

const updateBrokerEpic = action$ =>
  action$.pipe(
    ofType(actions.updateBroker.TRIGGER),
    map(action => action.payload),
    switchMap(values =>
      from(brokerApi.update(values)).pipe(
        map(res => normalize(res.data, schema.broker)),
        map(entities => actions.updateBroker.success(entities)),
        startWith(actions.updateBroker.request()),
        catchError(res => of(actions.updateBroker.failure(res))),
      ),
    ),
  );

const startBrokerEpic = action$ =>
  action$.pipe(
    ofType(actions.startBroker.TRIGGER),
    map(action => action.payload),
    switchMap(params =>
      from(brokerApi.start(params)).pipe(
        map(res => normalize(res.data, schema.broker)),
        map(entities => actions.startBroker.success(entities)),
        startWith(actions.startBroker.request()),
        catchError(res => of(actions.startBroker.failure(res))),
      ),
    ),
  );

const stopBrokerEpic = action$ =>
  action$.pipe(
    ofType(actions.stopBroker.TRIGGER),
    map(action => action.payload),
    switchMap(params =>
      from(brokerApi.stop(params)).pipe(
        map(res => normalize(res.data, schema.broker)),
        map(entities => actions.stopBroker.success(entities)),
        startWith(actions.stopBroker.request()),
        catchError(res => of(actions.stopBroker.failure(res))),
      ),
    ),
  );

const deleteBrokerEpic = action$ =>
  action$.pipe(
    ofType(actions.deleteBroker.TRIGGER),
    map(action => action.payload),
    switchMap(params =>
      from(brokerApi.remove(params)).pipe(
        map(params => getId(params)),
        map(id => actions.deletePipeline.success(id)),
        startWith(actions.deleteBroker.request()),
        catchError(res => of(actions.deleteBroker.failure(res))),
      ),
    ),
  );

export default combineEpics(
  fetchBrokerEpic,
  createBrokerEpic,
  updateBrokerEpic,
  startBrokerEpic,
  stopBrokerEpic,
  deleteBrokerEpic,
);
