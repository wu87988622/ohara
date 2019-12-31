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
import _ from 'lodash';

import * as connectorApi from 'api/connectorApi';
import * as streamApi from 'api/streamApi';

export const useDeleteServices = () => {
  const [steps, setSteps] = React.useState([]);
  const [activeStep, setActiveStep] = React.useState(0);

  const deleteServices = async services => {
    const runningServices = services.filter(
      service => service.kind !== 'topic' && Boolean(service.state),
    );

    // If there's no running objects, don't display the process bar
    if (!_.isEmpty(runningServices)) {
      setSteps([...services.map(object => object.name)]);
    }

    // Need to use a while loop so we can update
    // react state: `activeStep` in the loop
    let index = 0;
    while (index < services.length) {
      const service = services[index];
      const { kind, name, group } = service;
      const isRunning = Boolean(service.state);
      const isConnector = kind === 'source' || kind === 'sink';

      // Connectors and stream apps are the only services that
      // we're going to delete
      if (isConnector) {
        if (isRunning) await connectorApi.stop({ group, name });

        await connectorApi.remove({ group, name });
      }

      if (kind === 'stream') {
        if (isRunning) await streamApi.stop({ group, name });

        await streamApi.remove({ group, name });
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
