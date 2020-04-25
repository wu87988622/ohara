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
import { ofType } from 'redux-observable';
import { of, defer } from 'rxjs';
import * as _ from 'lodash';
import { catchError, map, startWith, mergeMap } from 'rxjs/operators';

import * as connectorApi from 'api/connectorApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { KIND } from 'const';

export default action$ => {
  return action$.pipe(
    ofType(actions.updateConnector.TRIGGER),
    map(action => action.payload),
    mergeMap(({ params, options }) => {
      const hasTopicKey = params.topicKeys.length > 0;
      const { cell, paperApi, topics } = options;
      const cells = paperApi.getCells();
      const connectorId = paperApi.getCell(params.name).id;

      return defer(() => connectorApi.update(params)).pipe(
        map(res => normalize(res.data, schema.connector)),
        map(normalizedData => {
          const currentHasTopicKey =
            _.get(normalizedData, 'topicKeys', []).length > 0;
          if (currentHasTopicKey) {
            const topicId = paperApi.getCell(normalizedData.topicKeys[0].name)
              .id;
            let linkId;
            switch (cell.kind) {
              case KIND.source:
                linkId = cells
                  .filter(cell => cell.cellType === 'standard.Link')
                  .find(
                    cell =>
                      cell.sourceId === connectorId &&
                      cell.targetId === topicId,
                  ).id;
                break;
              case KIND.sink:
                linkId = cells
                  .filter(cell => cell.cellType === 'standard.Link')
                  .find(
                    cell =>
                      cell.sourceId === topicId &&
                      cell.targetId === connectorId,
                  ).id;
                break;
              default:
                break;
            }
            paperApi.removeLink(linkId);
          }
          if (hasTopicKey) {
            switch (cell.kind) {
              case KIND.source:
                paperApi.addLink(cell.id, topics[0].data.id);
                break;
              case KIND.sink:
                paperApi.addLink(topics[0].data.id, cell.id);
                break;
              default:
                break;
            }
          }
          return actions.updateConnector.success(
            _.merge(normalizedData, { connectorId }),
          );
        }),
        startWith(actions.updateConnector.request({ connectorId })),
        catchError(err => of(actions.updateConnector.failure(err))),
      );
    }),
  );
};
