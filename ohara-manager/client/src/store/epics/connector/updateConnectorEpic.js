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
import { from, defer } from 'rxjs';
import { catchError, map, startWith, mergeMap } from 'rxjs/operators';

import * as connectorApi from 'api/connectorApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { KIND } from 'const';
import { LOG_LEVEL } from 'const';

export default action$ => {
  return action$.pipe(
    ofType(actions.updateConnector.TRIGGER),
    map(action => action.payload),
    mergeMap(({ values, options }) => {
      const { paperApi } = options;
      const connectorId = paperApi.getCell(values.name).id;

      return defer(() => connectorApi.update(values)).pipe(
        map(res => normalize(res.data, schema.connector)),
        map(normalizedData => {
          handleSuccess(values, options);
          return actions.updateConnector.success(
            _.merge(normalizedData, { connectorId }),
          );
        }),
        startWith(actions.updateConnector.request({ connectorId })),
        catchError(err =>
          from([
            actions.updateConnector.failure(_.merge(err, { connectorId })),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]),
        ),
      );
    }),
  );
};

function handleSuccess(values, options) {
  if (options.paperApi) return;

  const { cell, paperApi, topics, connectors } = options;
  const cells = paperApi.getCells();
  const currentConnector = connectors.find(
    connector => connector.name === values.name,
  );
  const hasTopicKey = values.topicKeys.length > 0;
  const connectorId = paperApi.getCell(values.name).id;
  const currentHasTopicKey = currentConnector.topicKeys.length > 0;

  if (currentHasTopicKey) {
    const topicId = paperApi.getCell(currentConnector.topicKeys[0].name).id;
    let linkId;
    switch (cell.kind) {
      case KIND.source:
        linkId = cells
          .filter(cell => cell.cellType === 'standard.Link')
          .find(
            cell => cell.sourceId === connectorId && cell.targetId === topicId,
          ).id;
        break;
      case KIND.sink:
        linkId = cells
          .filter(cell => cell.cellType === 'standard.Link')
          .find(
            cell => cell.sourceId === topicId && cell.targetId === connectorId,
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
}
