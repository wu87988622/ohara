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

import { ofType } from 'redux-observable';
import { defer, timer, merge, from } from 'rxjs';
import {
  switchMap,
  map,
  startWith,
  catchError,
  takeUntil,
} from 'rxjs/operators';

import * as pipelineApi from 'api/pipelineApi';
import * as actions from 'store/actions';
import { KIND, LOG_LEVEL } from 'const';

export default action$ =>
  action$.pipe(
    ofType(actions.startUpdateMetrics.TRIGGER),
    map(action => action.payload),
    switchMap(({ params, options }) =>
      timer(0, 8000).pipe(
        switchMap(() =>
          defer(() => pipelineApi.get(params)).pipe(
            map(res => {
              const { objects } = res.data;
              updateMetrics(objects, options);
              return actions.startUpdateMetrics.success();
            }),
            startWith(actions.startUpdateMetrics.request()),
            catchError(err => {
              return from([
                actions.startUpdateMetrics.failure(err),
                actions.createEventLog.trigger({
                  ...err,
                  type: LOG_LEVEL.error,
                }),
              ]);
            }),
          ),
        ),
        takeUntil(
          merge(
            action$.pipe(
              ofType(
                actions.switchPipeline.TRIGGER,
                actions.stopUpdateMetrics.TRIGGER,
                actions.deletePipeline.SUCCESS,
                actions.startUpdateMetrics.FAILURE,
              ),
            ),
          ),
        ),
      ),
    ),
  );

function updateMetrics(objects, options) {
  const { pipelineObjectsRef, paperApi } = options;
  if (paperApi) {
    // Topic metrics are not displayed in Paper cell.
    const nodeMetrics = objects.filter(object => object.kind !== KIND.topic);

    paperApi.updateMetrics(nodeMetrics);

    // Cell doesn't contain metrics data so we're using a ref here to store
    // these objects and pass to PropertyView component
    pipelineObjectsRef.current = objects;
  }
}
