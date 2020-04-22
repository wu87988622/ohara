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

import * as _ from 'lodash';
import { normalize } from 'normalizr';
import { ofType } from 'redux-observable';
import { of, defer } from 'rxjs';
import { catchError, map, startWith, mergeMap } from 'rxjs/operators';

import * as shabondiApi from 'api/shabondiApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';

export default action$ => {
  return action$.pipe(
    ofType(actions.updateShabondi.TRIGGER),
    map(action => action.payload),
    mergeMap(({ params, options }) => {
      const hasSourceTopicKey =
        _.get(params, 'shabondi__source__toTopics', []).length > 0;
      const hasSinkTopicKey =
        _.get(params, 'shabondi__sink__fromTopics', []).length > 0;
      const { cell, paperApi, topics } = options;
      const cells = paperApi.getCells();
      const shabondiId = paperApi.getCell(params.name).id;

      return defer(() => shabondiApi.update(params)).pipe(
        map(res => normalize(res.data, schema.shabondi)),
        map(normalizedData => {
          const currentHasSourceTopicKey =
            _.get(normalizedData, 'shabondi__source__toTopics', []).length > 0;
          const currentHasSinkTopicKey =
            _.get(normalizedData, 'shabondi__sink__fromTopics', []).length > 0;

          if (currentHasSourceTopicKey) {
            const topicId = paperApi.getCell(
              normalizedData.shabondi__source__toTopics[0].name,
            ).id;
            const linkId = cells
              .filter(cell => cell.cellType === 'standard.Link')
              .find(
                cell =>
                  cell.sourceId === shabondiId && cell.targetId === topicId,
              ).id;
            paperApi.removeLink(linkId);
          }
          if (currentHasSinkTopicKey) {
            const topicId = paperApi.getCell(
              normalizedData.shabondi__sink__fromTopics[0].name,
            ).id;
            const linkId = cells
              .filter(cell => cell.cellType === 'standard.Link')
              .find(
                cell =>
                  cell.sourceId === topicId && cell.targetId === shabondiId,
              ).id;
            paperApi.removeLink(linkId);
          }

          if (hasSourceTopicKey) paperApi.addLink(cell.id, topics[0].data.id);
          if (hasSinkTopicKey) paperApi.addLink(topics[0].data.id, cell.id);

          return actions.updateShabondi.success(
            _.merge(normalizedData, { shabondiId }),
          );
        }),
        startWith(actions.updateShabondi.request({ shabondiId })),
        catchError(err => of(actions.updateShabondi.failure(err))),
      );
    }),
  );
};
