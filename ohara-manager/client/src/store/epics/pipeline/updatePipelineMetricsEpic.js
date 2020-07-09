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
  catchError,
  ignoreElements,
  takeUntil,
  filter,
} from 'rxjs/operators';

import * as pipelineApi from 'api/pipelineApi';
import * as actions from 'store/actions';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import { KIND, LOG_LEVEL } from 'const';

export default (action$) =>
  action$.pipe(
    ofType(actions.startUpdateMetrics.TRIGGER),
    map((action) => action.payload),
    switchMap(({ params, options = {} }) =>
      timer(0, 8000).pipe(
        switchMap(() => {
          return defer(() => pipelineApi.get(params)).pipe(
            filter((res) => {
              const { objects } = res.data;
              const hasRunningServices = objects
                .filter((object) => object.kind !== KIND.topic)
                .some((object) => object.state === SERVICE_STATE.RUNNING);
              return hasRunningServices;
            }),
            map((res) => {
              const { objects } = res.data;
              options.updatePipelineMetrics(objects);
            }),
            // Don't dispatch an action from here. Doing so to prevent this epic from
            // updating other components during metrics update
            ignoreElements(),
          );
        }),
        takeUntil(
          merge(
            action$.pipe(
              ofType(
                actions.switchPipeline.TRIGGER,
                actions.deletePipeline.SUCCESS,
                actions.stopUpdateMetrics.TRIGGER,
                actions.startUpdateMetrics.FAILURE,
              ),
            ),
          ),
        ),
        catchError((err) => {
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
  );
