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

import _ from 'lodash';
import { normalize } from 'normalizr';
import { ofType } from 'redux-observable';
import { defer, from } from 'rxjs';
import { catchError, map, startWith, mergeMap } from 'rxjs/operators';

import * as streamApi from 'api/streamApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';
import { LOG_LEVEL } from 'const';

export default action$ =>
  action$.pipe(
    ofType(actions.updateStream.TRIGGER),
    map(action => action.payload),
    mergeMap(({ values, options }) => {
      const streamId = getId(values);
      return defer(() => streamApi.update(values)).pipe(
        map(res => normalize(res.data, schema.stream)),
        map(normalizedData => {
          handleSuccess(values, options);
          return actions.updateStream.success(
            _.merge(normalizedData, { streamId }),
          );
        }),
        startWith(actions.updateStream.request({ streamId })),
        catchError(error =>
          from([
            actions.updateStream.failure(_.merge(error, { streamId })),
            actions.createEventLog.trigger({ ...error, type: LOG_LEVEL.error }),
          ]),
        ),
      );
    }),
  );

function handleSuccess(values, options) {
  if (!options.paperApi) return;

  const { paperApi, cell, topics, streams } = options;
  const cells = paperApi.getCells();

  const currentStream = streams.find(stream => stream.name === values.name);
  const hasTo = values.to?.length > 0;
  const hasFrom = values.from?.length > 0;
  const currentHasTo = currentStream.to?.length > 0;
  const currentHasFrom = currentStream.from?.length > 0;

  if (currentHasTo) {
    const streamId = paperApi.getCell(values.name).id;
    const topicId = paperApi.getCell(currentStream.to[0].name).id;
    const linkId = cells
      .filter(cell => cell.cellType === 'standard.Link')
      .find(cell => cell.sourceId === streamId && cell.targetId === topicId).id;
    paperApi.removeLink(linkId);
  }

  if (currentHasFrom) {
    const streamId = paperApi.getCell(values.name).id;
    const topicId = paperApi.getCell(currentStream.from[0].name).id;
    const linkId = cells
      .filter(cell => cell.cellType === 'standard.Link')
      .find(cell => cell.sourceId === topicId && cell.targetId === streamId).id;

    paperApi.removeLink(linkId);
  }

  if (hasTo) {
    paperApi.addLink(cell.id, topics.find(topic => topic.key === 'to').data.id);
  }

  if (hasFrom) {
    paperApi.addLink(
      topics.find(topic => topic.key === 'from').data.id,
      cell.id,
    );
  }
}
