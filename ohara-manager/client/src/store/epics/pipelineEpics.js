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
import { map, startWith, switchMap, catchError } from 'rxjs/operators';

import * as pipelineApi from 'api/pipelineApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';

const fetchPipelineEpic = action$ =>
  action$.pipe(
    ofType(actions.fetchPipeline.TRIGGER),
    map(action => action.payload),
    switchMap(values =>
      from(pipelineApi.get(values)).pipe(
        map(res => normalize(res.data, schema.pipeline)),
        map(entities => actions.fetchPipeline.success(entities)),
        startWith(actions.fetchPipeline.request()),
        catchError(res => of(actions.fetchPipeline.failure(res))),
      ),
    ),
  );

const createPipelineEpic = action$ =>
  action$.pipe(
    ofType(actions.createPipeline.TRIGGER),
    map(action => action.payload),
    switchMap(values =>
      from(pipelineApi.create(values)).pipe(
        map(res => normalize(res.data, schema.pipeline)),
        map(entities => actions.createPipeline.success(entities)),
        startWith(actions.createPipeline.request()),
        catchError(res => of(actions.createPipeline.failure(res))),
      ),
    ),
  );

const updatePipelineEpic = action$ =>
  action$.pipe(
    ofType(actions.updatePipeline.TRIGGER),
    map(action => action.payload),
    switchMap(values =>
      from(pipelineApi.update(values)).pipe(
        map(res => normalize(res.data, schema.pipeline)),
        map(entities => actions.updatePipeline.success(entities)),
        startWith(actions.updatePipeline.request()),
        catchError(res => of(actions.updatePipeline.failure(res))),
      ),
    ),
  );

const deletePipelineEpic = action$ =>
  action$.pipe(
    ofType(actions.deletePipeline.TRIGGER),
    map(action => action.payload),
    switchMap(params =>
      from(pipelineApi.remove(params)).pipe(
        map(params => getId(params)),
        map(id => actions.deletePipeline.success(id)),
        startWith(actions.deletePipeline.request()),
        catchError(res => of(actions.deletePipeline.failure(res))),
      ),
    ),
  );

export default combineEpics(
  fetchPipelineEpic,
  createPipelineEpic,
  updatePipelineEpic,
  deletePipelineEpic,
);
