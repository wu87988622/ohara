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
import { CELL_TYPES } from 'const';

const pipeline = () => {
  const currentPipelineName = hooks.usePipelineName();
  const updatePipeline = hooks.useUpdatePipelineAction();

  const updateCells = (paperApi) => {
    const cellsJson = {
      cells: paperApi.toJson().cells.filter((cell) => !cell.isTemporary),
    };

    const endpoints = cellsJson.cells
      .filter((cell) => cell.type === CELL_TYPES.ELEMENT)
      .map((cell) => {
        return { name: cell.name, kind: cell.kind };
      });

    updatePipeline({
      name: currentPipelineName,
      endpoints,
      tags: {
        ...cellsJson,
      },
    });
  };

  const getUpdatedCells = (pipeline) => {
    const {
      tags: { cells = [] },
      objects,
    } = pipeline;

    const updatedCells = cells.map((cell) => {
      const currentObject = objects.find((object) => object.name === cell.name);

      // Ensure we're getting the latest status from the backend APIs
      if (currentObject) {
        return {
          ...cell,
          // TODO: use a more robust way to do the check
          // Normally, object won't have error field and so we're using this to
          // determine if the given object doesn't contain class definitions
          isIllegal: !!currentObject?.error,
          status: currentObject.state,
        };
      }

      return cell;
    });

    return updatedCells;
  };

  return { updateCells, getUpdatedCells };
};

export default pipeline;
