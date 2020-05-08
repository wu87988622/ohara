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
import { of, defer, from, throwError, iif } from 'rxjs';
import {
  catchError,
  map,
  switchMap,
  startWith,
  delay,
  concatAll,
  retryWhen,
  concatMap,
  mergeMap,
} from 'rxjs/operators';

import * as pipelineApi from 'api/pipelineApi';
import * as streamApi from 'api/streamApi';
import * as connectorApi from 'api/connectorApi';
import * as topicApi from 'api/topicApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { KIND, CELL_STATUS, LOG_LEVEL } from 'const';

const deleteStream$ = (params, paperApi) => {
  const { id, name, group } = params;
  return defer(() => streamApi.remove({ name, group })).pipe(
    map(() => {
      paperApi.removeElement(id, { skipGraphEvents: true });
      return actions.deleteStream.success({ name, group });
    }),
  );
};

const deleteAllStreams$ = (streams, paperApi) => {
  return of(...streams).pipe(
    mergeMap(stream => deleteStream$(stream, paperApi)),
  );
};

const deleteConnector$ = (params, paperApi) => {
  const { id, name, group } = params;
  return defer(() => connectorApi.remove({ name, group })).pipe(
    map(() => {
      paperApi.removeElement(id, { skipGraphEvents: true });
      return actions.deleteConnector.success({ name, group });
    }),
  );
};

const deleteAllConnectors$ = (connectors, paperApi) => {
  return of(...connectors).pipe(
    mergeMap(connector => deleteConnector$(connector, paperApi)),
  );
};

const stopTopic$ = params => {
  return defer(() => topicApi.stop(params)).pipe(
    map(() => actions.stopTopic.request(params)),
  );
};

const waitUntilTopicStopped$ = (params, paperApi) => {
  const { id, name, group } = params;
  return defer(() => topicApi.get({ name, group })).pipe(
    map(res => {
      if (res.data.state) throw res;

      paperApi.updateElement(
        id,
        { status: CELL_STATUS.stopped },
        { skipGraphEvents: true },
      );
      return actions.stopTopic.success(res.data);
    }),
    retryWhen(errors =>
      errors.pipe(
        concatMap((value, index) =>
          iif(
            () => index > 2,
            throwError({ title: 'stop topic exceeded max retry count' }),
            of(value).pipe(delay(2000)),
          ),
        ),
      ),
    ),
  );
};

const deleteTopic$ = (params, paperApi) => {
  const { id, name, group } = params;
  return defer(() => topicApi.remove({ name, group })).pipe(
    map(() => {
      paperApi.removeElement(id, { skipGraphEvents: true });
      return actions.deleteTopic.success({ name, group });
    }),
  );
};

const stopAndDeleteAllTopics$ = (topics, paperApi) => {
  return of(...topics).pipe(
    mergeMap(topic => {
      // Allow users to delete topics that don't have the "correct status" like pending or stopped since normally, topic status should always be running in our UI
      if (
        topic.status.toLowerCase() === CELL_STATUS.stopped ||
        topic.status.toLowerCase() === CELL_STATUS.pending
      ) {
        return deleteTopic$(topic, paperApi);
      }

      return of(
        stopTopic$(topic),
        waitUntilTopicStopped$(topic, paperApi),
        deleteTopic$(topic, paperApi),
      ).pipe(concatAll());
    }),
  );
};

const deletePipeline$ = params =>
  defer(() => pipelineApi.remove(params)).pipe(
    mergeMap(() => {
      return from([
        actions.deletePipeline.success(getId(params)),
        actions.switchPipeline.trigger(),
      ]);
    }),
    startWith(actions.deletePipeline.request()),
  );

export default action$ =>
  action$.pipe(
    ofType(actions.deletePipeline.TRIGGER),
    map(action => action.payload),
    switchMap(({ params, options }) => {
      const { name, group, cells } = params;
      const { paperApi } = options;
      const streams = cells.filter(cell => cell.kind === KIND.stream);
      const connectors = cells.filter(
        cell => cell.kind === KIND.source || cell.kind === KIND.sink,
      );
      const topics = cells.filter(
        cell => cell.kind === KIND.topic && !cell.isShared,
      );

      return of(
        deleteAllConnectors$(connectors, paperApi),
        deleteAllStreams$(streams, paperApi),
        stopAndDeleteAllTopics$(topics, paperApi),
        deletePipeline$({ group, name }),
      ).pipe(
        concatAll(),
        catchError(err => {
          return from([
            actions.deletePipeline.failure(err),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]);
        }),
      );
    }),
  );
