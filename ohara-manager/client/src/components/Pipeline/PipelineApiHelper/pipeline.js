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

const pipeline = () => {
  const { updatePipeline } = context.usePipelineActions();
  const { currentPipeline } = context.useWorkspace();

  const update = async params => {
    await updatePipeline({ ...params });
  };

  const updateCells = paperApi => {
    update({
      name: currentPipeline.name,
      tags: {
        ...paperApi.toJSON(),
      },
    });
  };

  const addEndpoint = params => {
    const { name, kind } = params;
    update({
      name: currentPipeline.name,
      endpoints: [
        ...currentPipeline.endpoints,
        {
          name,
          kind,
        },
      ],
    });
  };

  const removeEndpoint = params => {
    const { name, kind } = params;
    update({
      name: currentPipeline.name,
      endpoints: currentPipeline.endpoints.filter(
        endpoint => endpoint.name !== name && endpoint.kind !== kind,
      ),
    });
  };

  return { update, addEndpoint, removeEndpoint, updateCells };
};

export default pipeline;
