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

import { Observable, defer, forkJoin, of, throwError, zip } from 'rxjs';
import { catchError, map, mergeMap } from 'rxjs/operators';
import { retryBackoff } from 'backoff-rxjs';
import { isEmpty } from 'lodash';
import { ObjectKey } from 'api/apiInterface/basicInterface';
import { TopicData } from 'api/apiInterface/topicInterface';
import * as topicApi from 'api/topicApi';
import { RETRY_CONFIG } from 'const';
import { getKey } from 'utils/object';
import { hashByGroupAndName } from 'utils/sha';

export function createTopic(values: any): Observable<TopicData> {
  return defer(() => topicApi.create(values)).pipe(map((res) => res.data));
}

export function fetchTopic(values: ObjectKey): Observable<TopicData> {
  return defer(() => topicApi.get(values)).pipe(map((res) => res.data));
}

export function fetchTopics(workspaceKey: ObjectKey): Observable<TopicData[]> {
  const topicGroup = hashByGroupAndName(workspaceKey.group, workspaceKey.name);
  return defer(() => topicApi.getAll({ group: topicGroup })).pipe(
    map((res) => res.data),
  );
}

export function startTopic(key: ObjectKey): Observable<TopicData> {
  return zip(
    // attempt to start at intervals
    defer(() => topicApi.start(key)),
    // wait until the service is running
    defer(() => topicApi.get(key)).pipe(
      map((res: any) => {
        if (res?.data?.state === 'RUNNING') return res.data;
        throw res;
      }),
    ),
  ).pipe(
    map(([, data]) => data),
    // retry every 2 seconds, up to 10 times
    retryBackoff(RETRY_CONFIG),
    catchError((error) =>
      throwError({
        data: error?.data,
        meta: error?.meta,
        title:
          `Try to start topic: "${key.name}" failed after retry ${RETRY_CONFIG.maxRetries} times. ` +
          `Expected state: RUNNING, Actual state: ${error.data.state}`,
      }),
    ),
  );
}

export function stopTopic(key: ObjectKey): Observable<TopicData> {
  return zip(
    // attempt to stop at intervals
    defer(() => topicApi.stop(key)),
    // wait until the service is not running
    defer(() => topicApi.get(key)).pipe(
      map((res: any) => {
        if (!res?.data?.state) return res.data;
        throw res;
      }),
    ),
  ).pipe(
    map(([, data]) => data),
    // retry every 2 seconds, up to 10 times
    retryBackoff(RETRY_CONFIG),
    catchError((error) =>
      throwError({
        data: error?.data,
        meta: error?.meta,
        title:
          `Try to stop topic: "${key.name}" failed after retry ${RETRY_CONFIG.maxRetries} times. ` +
          `Expected state is nonexistent, Actual state: ${error.data.state}`,
      }),
    ),
  );
}

export function deleteTopic(key: ObjectKey) {
  return defer(() => topicApi.remove(key));
}

// Fetch and stop all topics for this workspace
export function fetchAndStopTopics(
  workspaceKey: ObjectKey,
): Observable<TopicData[]> {
  return fetchTopics(workspaceKey).pipe(
    mergeMap((topics) => {
      if (isEmpty(topics)) return of(topics);
      return forkJoin(topics.map((topic) => stopTopic(getKey(topic))));
    }),
  );
}

// Fetch and delete all topics for this workspace
export function fetchAndDeleteTopics(workspaceKey: ObjectKey) {
  return fetchTopics(workspaceKey).pipe(
    mergeMap((topics) => {
      if (isEmpty(topics)) return of(topics);
      return forkJoin(topics.map((topic) => deleteTopic(getKey(topic))));
    }),
  );
}
