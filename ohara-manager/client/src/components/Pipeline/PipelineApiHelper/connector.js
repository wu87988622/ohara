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

import * as context from 'context';
import * as util from './apiHelperUtils';
import { CELL_STATUS, KIND } from 'const';
import pipeline from './pipeline';

const connector = () => {
  const {
    createConnector,
    updateConnector,
    startConnector,
    stopConnector,
    deleteConnector,
  } = context.useConnectorActions();

  const create = async (params, paperApi) => {
    const { id, name, className, kind } = params;
    const res = await createConnector({
      name,
      connector__class: className,
    });
    if (!res.error) {
      const state = util.getCellState(res);
      paperApi.updateElement(id, {
        status: state,
      });
      pipeline().addEndpoint({ name, kind });
    } else {
      paperApi.removeElement(id);
    }
  };

  const update = async (cell, topic, values, paperApi) => {
    const res = await updateConnector({
      name: cell.name,
      ...values,
    });

    if (!res.error && topic !== undefined) {
      switch (cell.kind) {
        case KIND.source:
          paperApi.addLink(cell.id, topic.id);
          break;
        case KIND.sink:
          paperApi.addLink(topic.id, cell.id);
          break;
        default:
          break;
      }
    }
    return res;
  };

  const start = async (params, paperApi) => {
    const { id, name } = params;

    paperApi.updateElement(id, {
      status: CELL_STATUS.pending,
    });
    const res = await startConnector(name);
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
    const res = await stopConnector(name);
    if (!res.error) {
      const state = util.getCellState(res);
      paperApi.updateElement(id, {
        status: state,
      });
    }
  };

  const remove = async (params, paperApi) => {
    const { id, name, kind } = params;
    const res = await deleteConnector(name);
    if (!res.error) {
      paperApi.removeElement(id);
      pipeline().removeEndpoint({ name, kind });
    }
  };

  const updateLink = async (params, paperApi) => {
    const { connector, topic, link } = params;

    const res = await updateConnector({
      name: connector.name,
      topicKeys: [{ name: topic.name }],
    });

    if (res.error) {
      paperApi.removeElement(link.id);
    }
    return res;
  };

  const removeSourceLink = async (params, topic, paperApi) => {
    const { name, id } = params;
    const res = await updateConnector({
      name,
      topicKeys: [],
    });

    if (res.error) {
      paperApi.addLink(id, topic.id);
    }
  };

  const removeSinkLink = async (params, topic, paperApi) => {
    const { name, id } = params;
    const res = await updateConnector({
      name,
      topicKeys: [],
    });

    if (res.error) {
      paperApi.addLink(topic.id, id);
    }
  };

  return {
    create,
    update,
    start,
    stop,
    remove,
    updateLink,
    removeSourceLink,
    removeSinkLink,
  };
};

export default connector;
