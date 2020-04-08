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

import React from 'react';

import { KIND, CELL_STATUS } from 'const';
import { useConnectorActions } from 'context';
import * as hooks from 'hooks';
import { PaperContext } from '../Pipeline';
import { sleep } from 'utils/common';

export const useDeleteCells = () => {
  const [steps, setSteps] = React.useState([]);
  const [activeStep, setActiveStep] = React.useState(0);

  const { deleteConnector, stopConnector } = useConnectorActions();
  const stopAndDeleteTopic = hooks.useStopAndDeleteTopicAction();
  const stopAndDeleteStream = hooks.useStopAndDeleteStreamAction();

  const deleteCells = async cells => {
    const cellNames = cells.map(cell => {
      if (cell.kind !== KIND.topic) return cell.name;
      return cell.isShared ? cell.name : cell.displayName;
    });

    setSteps(cellNames);

    // Need to use a while loop so we can update
    // react state: `activeStep` in the loop
    let index = 0;
    while (index < cells.length) {
      const currentCell = cells[index];
      const { kind, isShared, name, status } = currentCell;

      if (kind === KIND.source || kind === KIND.sink) {
        // To ensure services can be properly removed, we're stopping the services
        // here no matter if it's running or not
        await stopConnector(name);
        await deleteConnector(name);
      }

      if (kind === KIND.stream) {
        stopAndDeleteStream({ name });
      }

      // Only pipeline-only topics are belong to this Pipeline and so need to
      // be deleted along with this pipeline
      if (kind === KIND.topic && !isShared) {
        stopAndDeleteTopic({ name });
      }

      index++;
      setActiveStep(index);

      // TODO: A temp fix as for deleting a bunch components from the Paper altogether
      // often would cause trouble for backend, so we're making the request in a less
      // frequent manner
      if (status.toLowerCase() === CELL_STATUS.running) {
        await sleep(1500);
      }
    }
  };

  return {
    deleteCells,
    steps,
    activeStep,
  };
};

export const useZoom = () => {
  const [paperScale, setPaperScale] = React.useState(1); // defaults to `1` -> 100%
  const paperApi = React.useContext(PaperContext);

  const setZoom = (scale, instruction) => {
    const fixedScale = Number((Math.floor(scale * 100) / 100).toFixed(2));
    const allowedScales = [
      0.25,
      0.5,
      0.67,
      0.75,
      0.8,
      0.9,
      1.0,
      1.1,
      1.25,
      1.5,
    ];
    const isValidScale = allowedScales.includes(fixedScale);

    if (isValidScale) {
      // If the instruction is `fromDropdown`, we will use the scale it gives
      // and update the state right alway
      if (instruction === 'fromDropdown') {
        paperApi.setScale(scale);
        return setPaperScale(scale);
      }

      const scaleIndex = allowedScales.indexOf(fixedScale);
      let newScale;

      // in => zoom in
      // out => zoom out
      if (instruction === 'in') {
        newScale =
          scaleIndex >= allowedScales.length
            ? allowedScales[scaleIndex]
            : allowedScales[scaleIndex + 1];
      } else {
        newScale =
          scaleIndex <= 0
            ? allowedScales[scaleIndex]
            : allowedScales[scaleIndex - 1];
      }

      paperApi.setScale(newScale);
      return setPaperScale(newScale);
    }

    // Handle `none-valid` scales here
    const defaultScales = [0.5, 0.75, 1.0, 1.5];
    const closest = defaultScales.reduce((prev, curr) => {
      return Math.abs(curr - fixedScale) < Math.abs(prev - fixedScale)
        ? curr
        : prev;
    });

    let outScale;
    let inScale;
    if (closest === 0.5) {
      inScale = fixedScale <= 0.5 ? 0.5 : 0.75;
      outScale = 0.5;
    } else if (closest === 0.75) {
      inScale = fixedScale <= 0.75 ? 0.75 : 1;
      outScale = fixedScale >= 0.75 ? 0.75 : 0.5;
    } else if (closest === 1) {
      inScale = 1;
      outScale = 0.75;
    }

    const newScale = instruction === 'in' ? inScale : outScale;
    paperApi.setScale(newScale);
    setPaperScale(newScale);
    return newScale;
  };

  return {
    setZoom,
    scale: paperScale,
    setScale: setPaperScale,
  };
};
