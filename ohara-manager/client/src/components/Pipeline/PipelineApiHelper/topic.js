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

import * as hooks from 'hooks';
import { CELL_STATUS } from 'const';

const topic = () => {
  const createAndStartTopic = hooks.useCreateAndStartTopicAction();
  const stopAndDeleteTopic = hooks.useStopAndDeleteTopicAction();
  const toolBoxTopics = hooks.useTopicsInToolbox();

  const createAndStart = (params, paperApi) => {
    const { id, name, displayName, isShared } = params;

    if (isShared) {
      const target = toolBoxTopics.find((topic) => topic.name === name);
      return paperApi.updateElement(id, {
        status: target.state.toLowerCase(),
      });
    }

    createAndStartTopic(
      {
        id,
        name,
        displayName,
        isShared: false,
      },
      { paperApi },
    );
  };

  const stopAndRemove = (params, paperApi) => {
    const { id, name, isShared } = params;

    if (isShared) return paperApi.removeElement(id);

    paperApi.updateElement(id, {
      status: CELL_STATUS.pending,
    });

    stopAndDeleteTopic(
      {
        id,
        name,
      },
      { paperApi },
    );
  };

  return { createAndStart, stopAndRemove };
};

export default topic;
