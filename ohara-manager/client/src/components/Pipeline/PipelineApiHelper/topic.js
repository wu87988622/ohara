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
import { CELL_STATUS } from 'const';

const topic = () => {
  const { createTopic, stopTopic, deleteTopic } = context.useTopicActions();
  const { data: topics } = context.useTopicState();

  const create = async (params, paperApi) => {
    const { id, name, displayName, isShared } = params;

    if (isShared) {
      const target = topics.find(topic => topic.name === name);
      return paperApi.updateElement(id, {
        status: target.state.toLowerCase(),
      });
    }

    paperApi.updateElement(id, {
      status: CELL_STATUS.pending,
    });

    paperApi.disableMenu(id);

    const res = await createTopic({
      name,
      tags: {
        isShared: false,
        displayName,
      },
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
    return res;
  };

  const remove = async (params, paperApi) => {
    const { id, name, isShared } = params;

    if (isShared) return paperApi.removeElement(id);

    paperApi.updateElement(id, {
      status: CELL_STATUS.pending,
    });

    paperApi.disableMenu(id);
    const stopRes = await stopTopic(name);
    paperApi.enableMenu(id);

    if (!stopRes.error) {
      const state = util.getCellState(stopRes);
      paperApi.updateElement(id, {
        status: state,
      });
    }

    const deleteRes = await deleteTopic(name);
    if (!deleteRes.error) {
      paperApi.removeElement(id);
    }
  };

  return { create, remove };
};

export default topic;
