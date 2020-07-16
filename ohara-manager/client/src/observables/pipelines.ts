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

import { Observable, defer, forkJoin, of } from 'rxjs';
import { map, mapTo, mergeMap } from 'rxjs/operators';
import { isEmpty } from 'lodash';

import { ObjectKey } from 'api/apiInterface/basicInterface';
import * as pipelineApi from 'api/pipelineApi';
import { Pipeline } from 'api/apiInterface/pipelineInterface';
import { getKey } from 'utils/object';
import { hashByGroupAndName } from 'utils/sha';

export function fetchPipelines(
  workspaceKey: ObjectKey,
): Observable<Pipeline[]> {
  const pipelineGroup = hashByGroupAndName(
    workspaceKey.group,
    workspaceKey.name,
  );
  return defer(() => pipelineApi.getAll({ group: pipelineGroup })).pipe(
    map((res) => res.data),
  );
}

export function deletePipeline(key: ObjectKey) {
  return defer(() => pipelineApi.remove(key));
}

// Fetch and delete all pipelines for this workspace
export function fetchAndDeletePipelines(workspaceKey: ObjectKey) {
  return fetchPipelines(workspaceKey).pipe(
    mergeMap((pipelines) => {
      if (isEmpty(pipelines)) return of(pipelines);
      return forkJoin(
        pipelines.map((pipeline) =>
          deletePipeline(getKey(pipeline)).pipe(mapTo(pipeline)),
        ),
      );
    }),
  );
}
