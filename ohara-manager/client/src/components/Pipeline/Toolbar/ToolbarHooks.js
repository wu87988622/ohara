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

import { KIND } from 'const';
import {
  useConnectorActions,
  useTopicActions,
  useStreamActions,
} from 'context';
import { PaperContext } from '../Pipeline';

export const useDeleteServices = () => {
  const [steps, setSteps] = React.useState([]);
  const [activeStep, setActiveStep] = React.useState(0);
  const { deleteConnector, stopConnector } = useConnectorActions();
  const { deleteTopic, stopTopic } = useTopicActions();
  const { deleteStream, stopStream } = useStreamActions();

  const deleteServices = async services => {
    setSteps([...services.map(object => object.name)]);

    // Need to use a while loop so we can update
    // react state: `activeStep` in the loop
    let index = 0;
    while (index < services.length) {
      const service = services[index];
      const { kind, name, tags } = service;
      const isRunning = Boolean(service.state);

      if (kind === KIND.source || kind === KIND.sink) {
        if (isRunning) await stopConnector(name);

        await deleteConnector(name);
      }

      // Only private topics are belong to this Pipeline and so need to
      // be deleted along with this pipeline
      const isPrivate = tags.type === 'private';
      if (kind === KIND.topic && isPrivate) {
        if (isRunning) await stopTopic(name);

        await deleteTopic(name);
      }

      if (kind === KIND.stream) {
        if (isRunning) await stopStream(name);

        await deleteStream(name);
      }

      index++;
      setActiveStep(index);
    }
  };

  return {
    deleteServices,
    steps,
    activeStep,
  };
};

export const useZoom = () => {
  const [paperScale, setPaperScale] = React.useState(1); // defaults to `1` -> 100%
  const paperApi = React.useContext(PaperContext);

  const setZoom = (scale, instruction) => {
    const fixedScale = Number((Math.floor(scale * 100) / 100).toFixed(2));
    const allowedScales = [0.01, 0.03, 0.06, 0.12, 0.25, 0.5, 1.0, 2.0];
    const isValidScale = allowedScales.includes(fixedScale);

    if (isValidScale) {
      // If the instruction is `fromDropdown`, we will use the scale it gives
      // and update the state right alway
      if (instruction === 'fromDropdown') return paperApi.setScale(scale);

      // By default, the scale is multiply and divide by `2`
      let newScale = 0;

      if (instruction === 'in') {
        // Manipulate two special values here, they're not valid
        // in our App:
        // 0.02 -> 0.03
        // 0.24 -> 0.25

        if (fixedScale * 2 === 0.02) {
          newScale = 0.03;
        } else if (fixedScale * 2 === 0.24) {
          newScale = 0.25;
        } else {
          // Handle other scale normally
          newScale = fixedScale * 2;
        }
      } else {
        newScale = fixedScale / 2;
      }

      paperApi.setScale(newScale);
      return setPaperScale(newScale);
    }

    // Handle `none-valid` scales here
    const defaultScales = [0.5, 1.0, 2.0];
    const closest = defaultScales.reduce((prev, curr) => {
      return Math.abs(curr - fixedScale) < Math.abs(prev - fixedScale)
        ? curr
        : prev;
    });

    let outScale;
    let inScale;
    if (closest === 0.5) {
      // If the fixedScale is something like 0.46, we'd like the next `in` scale
      // to be `0.5` not `1`
      inScale = fixedScale <= 0.5 ? 0.5 : 1;
      outScale = 0.5;
    } else if (closest === 1) {
      inScale = 1;
      outScale = 0.5;
    } else {
      inScale = 2;
      outScale = 2;
    }

    const newScale = instruction === 'in' ? inScale : outScale;
    paperApi.scale(newScale);
    setPaperScale(newScale);
    return newScale;
  };

  return {
    setZoom,
    scale: paperScale,
    setScale: setPaperScale,
  };
};
