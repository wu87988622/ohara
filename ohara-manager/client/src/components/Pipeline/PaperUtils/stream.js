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

const stream = () => {
  const {
    createStream,
    updateStream,
    startStream,
    stopStream,
    deleteStream,
  } = context.useStreamActions();

  const create = async params => {
    const { paperApi, id } = params;
    const name = util.getCellName(params);
    const connectorClass = util.getCellClassName(params);
    const jarKey = util.getCellJarKey(params);
    const res = await createStream({
      name,
      connector__class: connectorClass,
      jarKey,
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

  const update = async params => {
    const { data } = params;
    await updateStream({ ...data });
  };

  const start = async params => {
    const { paperApi, id, name } = params;
    paperApi.updateElement(id, {
      status: CELL_STATUS.pending,
    });
    const res = await startStream(name);
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

  const stop = async params => {
    const { paperApi, id, name } = params;
    const res = await stopStream(name);
    if (!res.error) {
      const state = util.getCellState(res);
      paperApi.updateElement(id, {
        status: state,
      });
    }
  };

  const remove = async params => {
    const { paperApi, id, name } = params;
    const res = await deleteStream(name);
    if (!res.error) {
      paperApi.removeElement(id);
    }
  };

  return { create, update, start, stop, remove };
};

export default stream;
