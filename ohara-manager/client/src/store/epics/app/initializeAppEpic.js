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

import { merge } from 'lodash';
import { normalize } from 'normalizr';
import { ofType } from 'redux-observable';
import { defer, from, forkJoin } from 'rxjs';
import { map, mergeMap, switchMap } from 'rxjs/operators';

import * as pipelineApi from 'api/pipelineApi';
import * as workspaceApi from 'api/workspaceApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { catchErrorWithEventLog } from '../utils';

export default (action$) =>
  action$.pipe(
    ofType(actions.initializeApp.TRIGGER),
    switchMap((action) => {
      const { workspaceName, pipelineName } = action.payload;
      return forkJoin(
        defer(() => workspaceApi.getAll()).pipe(map((res) => res.data)),
        defer(() => pipelineApi.getAll()).pipe(map((res) => res.data)),
      ).pipe(
        map(([workspaces, pipelines]) =>
          merge(
            normalize(workspaces, [schema.workspace]),
            normalize(pipelines, [schema.pipeline]),
          ),
        ),
        mergeMap((normalizedData) =>
          from([
            actions.initializeApp.success(normalizedData),
            actions.switchWorkspace.trigger({
              name: workspaceName,
              pipelineName,
            }),
            // we need to re-fetch the notification for AppBar
            actions.updateNotifications.trigger(),
          ]),
        ),
        catchErrorWithEventLog((err) => actions.initializeApp.failure(err)),
      );
    }),
  );
