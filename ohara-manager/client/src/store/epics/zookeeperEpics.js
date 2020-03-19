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

import * as zookeeperApi from 'api/zookeeperApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';

const fetchZookeeperEpic = action$ =>
  action$.pipe(
    ofType(actions.fetchZookeeper.TRIGGER),
    map(action => action.payload),
    switchMap(values =>
      from(zookeeperApi.get(values)).pipe(
        map(res => normalize(res.data, schema.zookeeper)),
        map(entities => actions.fetchZookeeper.success(entities)),
        startWith(actions.fetchZookeeper.request()),
        catchError(res => of(actions.fetchZookeeper.failure(res))),
      ),
    ),
  );

const createZookeeperEpic = action$ =>
  action$.pipe(
    ofType(actions.createZookeeper.TRIGGER),
    map(action => action.payload),
    switchMap(values =>
      from(zookeeperApi.create(values)).pipe(
        map(res => normalize(res.data, schema.zookeeper)),
        map(entities => actions.createZookeeper.success(entities)),
        startWith(actions.createZookeeper.request()),
        catchError(res => of(actions.createZookeeper.failure(res))),
      ),
    ),
  );

const updateZookeeperEpic = action$ =>
  action$.pipe(
    ofType(actions.updateZookeeper.TRIGGER),
    map(action => action.payload),
    switchMap(values =>
      from(zookeeperApi.update(values)).pipe(
        map(res => normalize(res.data, schema.zookeeper)),
        map(entities => actions.updateZookeeper.success(entities)),
        startWith(actions.updateZookeeper.request()),
        catchError(res => of(actions.updateZookeeper.failure(res))),
      ),
    ),
  );

const startZookeeperEpic = action$ =>
  action$.pipe(
    ofType(actions.startZookeeper.TRIGGER),
    map(action => action.payload),
    switchMap(params =>
      from(zookeeperApi.start(params)).pipe(
        map(res => normalize(res.data, schema.zookeeper)),
        map(entities => actions.startZookeeper.success(entities)),
        startWith(actions.startZookeeper.request()),
        catchError(res => of(actions.startZookeeper.failure(res))),
      ),
    ),
  );

const stopZookeeperEpic = action$ =>
  action$.pipe(
    ofType(actions.stopZookeeper.TRIGGER),
    map(action => action.payload),
    switchMap(params =>
      from(zookeeperApi.stop(params)).pipe(
        map(res => normalize(res.data, schema.zookeeper)),
        map(entities => actions.stopZookeeper.success(entities)),
        startWith(actions.stopZookeeper.request()),
        catchError(res => of(actions.stopZookeeper.failure(res))),
      ),
    ),
  );

const deleteZookeeperEpic = action$ =>
  action$.pipe(
    ofType(actions.deleteZookeeper.TRIGGER),
    map(action => action.payload),
    switchMap(params =>
      from(zookeeperApi.remove(params)).pipe(
        map(params => getId(params)),
        map(id => actions.deletePipeline.success(id)),
        startWith(actions.deleteZookeeper.request()),
        catchError(res => of(actions.deleteZookeeper.failure(res))),
      ),
    ),
  );

export default combineEpics(
  fetchZookeeperEpic,
  createZookeeperEpic,
  updateZookeeperEpic,
  startZookeeperEpic,
  stopZookeeperEpic,
  deleteZookeeperEpic,
);
