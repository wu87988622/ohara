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
import * as util from './paperUtils';
import { CELL_STATUS } from 'const';

const topic = () => {
  const { createTopic, stopTopic, deleteTopic } = context.useTopicActions();

  const create = async (params, paperApi) => {
    const { id, name } = params;
    const res = await createTopic({
      name,
    });
    paperApi.updateElement(id, {
      status: CELL_STATUS.pending,
    });
    if (!res.error) {
      const state = util.getCellState(res);
      paperApi.updateElement(id, {
        status: state,
      });
    } else {
      paperApi.removeElement(id);
    }
  };

  const remove = async (params, paperApi) => {
    const { id, name } = params;
    paperApi.updateElement(id, {
      status: CELL_STATUS.pending,
    });

    const stopRes = await stopTopic(name);
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
