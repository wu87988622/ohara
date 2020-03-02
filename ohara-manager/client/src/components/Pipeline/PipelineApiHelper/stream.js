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

import * as context from 'context';
import * as util from './apiHelperUtils';
import { CELL_STATUS } from 'const';

const stream = () => {
  const {
    createStream,
    updateStream,
    startStream,
    stopStream,
    deleteStream,
  } = context.useStreamActions();
  const { data: currentStreams } = context.useStreamState();

  const create = async (params, paperApi) => {
    const { id, name, className, jarKey } = params;
    paperApi.disableMenu(id);
    const res = await createStream({
      name,
      connector__class: className,
      jarKey,
    });

    paperApi.enableMenu(id);

    if (!res.error) {
      const state = util.getCellState(res);
      paperApi.updateElement(id, {
        status: state,
      });
    } else {
      paperApi.removeElement(id);
    }
  };

  const update = async (cell, topics, values, paperApi) => {
    const cells = paperApi.getCells();
    const res = await updateStream({
      name: cell.name,
      ...values,
    });

    if (!res.error) {
      const currentStream = currentStreams.find(
        stream => stream.name === values.name,
      );
      const hasTo = _.get(values, 'to', []).length > 0;
      const hasFrom = _.get(values, 'from', []).length > 0;
      const currentHasTo = _.get(currentStream, 'to', []).length > 0;
      const currentHasFrom = _.get(currentStream, 'from', []).length > 0;
      if (currentHasTo) {
        const streamId = paperApi.getCell(values.name).id;
        const topicId = paperApi.getCell(currentStream.to[0].name).id;
        const linkId = cells
          .filter(cell => cell.cellType === 'standard.Link')
          .find(cell => cell.sourceId === streamId && cell.targetId === topicId)
          .id;

        paperApi.removeLink(linkId);
      }
      if (currentHasFrom) {
        const streamId = paperApi.getCell(values.name).id;
        const topicId = paperApi.getCell(currentStream.from[0].name).id;
        const linkId = cells
          .filter(cell => cell.cellType === 'standard.Link')
          .find(cell => cell.sourceId === topicId && cell.targetId === streamId)
          .id;

        paperApi.removeLink(linkId);
      }
      if (hasTo) {
        paperApi.addLink(
          cell.id,
          topics.find(topic => topic.key === 'to').data.id,
        );
      }
      if (hasFrom) {
        paperApi.addLink(
          topics.find(topic => topic.key === 'from').data.id,
          cell.id,
        );
      }
    }
    return res;
  };

  const updateLinkTo = async (params, paperApi) => {
    const { toStream, topic, link } = params;
    const res = await updateStream({
      name: toStream.name,
      to: [{ name: topic.name }],
    });
    if (res.error) {
      paperApi.removeElement(link.id);
    }
    return res;
  };

  const updateLinkFrom = async (params, paperApi) => {
    const { fromStream, topic, link } = params;
    const res = await updateStream({
      name: fromStream.name,
      from: [{ name: topic.name }],
    });

    if (res.error) {
      paperApi.removeElement(link.id);
    }
    return res;
  };

  const start = async (params, paperApi) => {
    const { id, name } = params;
    paperApi.updateElement(id, {
      status: CELL_STATUS.pending,
    });
    paperApi.disableMenu(id);
    const res = await startStream(name);
    paperApi.enableMenu(id);
    if (!res.error) {
      const state = util.getCellState(res);
      paperApi.updateElement(id, {
        status: state,
      });
    } else {
      paperApi.updateElement(id, {
        status: CELL_STATUS.stopped,
      });
    }
  };

  const stop = async (params, paperApi) => {
    const { id, name } = params;
    paperApi.updateElement(id, {
      status: CELL_STATUS.pending,
    });
    paperApi.disableMenu(id);
    const res = await stopStream(name);
    paperApi.enableMenu(id);
    if (!res.error) {
      const state = util.getCellState(res);
      paperApi.updateElement(id, {
        status: state,
      });
    }
  };

  const remove = async (params, paperApi) => {
    const { id, name } = params;
    const res = await deleteStream(name);
    if (!res.error) {
      paperApi.removeElement(id);
    }
  };

  const removeLinkTo = async (params, topic, paperApi) => {
    const { name, id } = params;
    const res = await updateStream({
      name,
      to: [],
    });

    if (res.error) {
      paperApi.addLink(id, topic.id);
    }
  };

  const removeLinkFrom = async (params, topic, paperApi) => {
    const { name, id } = params;
    const res = await updateStream({
      name,
      from: [],
    });

    if (res.error) {
      paperApi.addLink(topic.id, id);
    }
  };

  return {
    create,
    update,
    updateLinkTo,
    updateLinkFrom,
    start,
    stop,
    remove,
    removeLinkTo,
    removeLinkFrom,
  };
};

export default stream;
