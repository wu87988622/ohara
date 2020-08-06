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
import { defer } from 'rxjs';
import { map, startWith, mergeMap } from 'rxjs/operators';

import * as shabondiApi from 'api/shabondiApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { CELL_TYPE } from 'const';
import { catchErrorWithEventLog } from '../utils';

export default (action$) => {
  return action$.pipe(
    ofType(actions.updateShabondi.TRIGGER),
    map((action) => action.payload),
    mergeMap(({ values, options }) => {
      const { paperApi } = options;
      const shabondiId = paperApi.getCell(values.name).id;

      return defer(() => shabondiApi.update(values)).pipe(
        map((res) => normalize(res.data, schema.shabondi)),
        map((normalizedData) => {
          handleSuccess(values, options);
          return actions.updateShabondi.success(
            _.merge(normalizedData, { shabondiId }),
          );
        }),
        startWith(actions.updateShabondi.request({ shabondiId })),
        catchErrorWithEventLog((err) =>
          actions.updateShabondi.failure(_.merge(err, { shabondiId })),
        ),
      );
    }),
  );
};

function handleSuccess(values, options) {
  if (!options.paperApi) return;

  const { cell, paperApi, topics, connectors } = options;
  const TO_TOPIC_KEY = 'shabondi__source__toTopics';
  const FROM_TOPIC_KEY = 'shabondi__sink__fromTopics';
  const currentShabondi = connectors.find(
    (connector) => connector.name === values.name,
  );
  const shabondiId = paperApi.getCell(values.name)?.id;

  const currentHasSourceTopicKey = currentShabondi?.[TO_TOPIC_KEY]?.length > 0;
  const currentHasSinkTopicKey = currentShabondi?.[FROM_TOPIC_KEY]?.length > 0;
  const hasSourceTopicKey = values[TO_TOPIC_KEY]?.length > 0;
  const hasSinkTopicKey = values[FROM_TOPIC_KEY]?.length > 0;
  const links = paperApi
    .getCells()
    .filter((cell) => cell.cellType === CELL_TYPE.LINK);

  if (currentHasSourceTopicKey) {
    const topicName = currentShabondi?.[TO_TOPIC_KEY][0].name;
    const topicId = paperApi.getCell(topicName)?.id;
    const linkId = links.find(
      (link) => link.sourceId === shabondiId && link.targetId === topicId,
    )?.id;

    paperApi.removeLink(linkId);
  }

  if (currentHasSinkTopicKey) {
    const topicName = currentShabondi?.[FROM_TOPIC_KEY][0].name;
    const topicId = paperApi.getCell(topicName)?.id;
    const linkId = links.find(
      (link) => link.sourceId === topicId && link.targetId === shabondiId,
    ).id;

    paperApi.removeLink(linkId);
  }

  if (hasSourceTopicKey) paperApi.addLink(cell.id, topics[0].data.id);
  if (hasSinkTopicKey) paperApi.addLink(topics[0].data.id, cell.id);
}
