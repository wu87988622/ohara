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

import { isObject } from 'lodash';

import * as connectorApi from 'api/connectorApi';
import { createProperty } from 'api/streamApi';

const getClassName = connector => {
  let className = '';

  if (isObject(connector)) {
    // TODO: figure out a better way to get topic class name
    className = connector.className || 'topic';
  } else {
    className = connector;
  }

  return className;
};

export const createConnector = async params => {
  const {
    updateGraph,
    connector,
    brokerClusterName,
    workerClusterKey,
    newConnectorName,
    newStreamName,
    showMessage,
    group,
  } = params;

  const { typeName } = connector;

  const className = getClassName(connector);
  let connectorName;

  if (typeName === 'topic') {
    // Topic is created beforehand therefore, a name is already exist.
    connectorName = connector.name;
  } else if (typeName === 'stream') {
    try {
      const response = await createProperty({
        jarKey: connector.jarKey,
        name: newStreamName,
        brokerClusterName,
        group,
      });

      if (!response.data.isSuccess) return; // failed to create

      connectorName = newStreamName;
    } catch (error) {
      showMessage(error.message);
    }
  } else if (typeName === 'source' || typeName === 'sink') {
    try {
      const response = await connectorApi.createConnector({
        name: newConnectorName,
        'connector.class': className,
        workerClusterKey,
        group,
      });

      if (!response.data.isSuccess) return; // failed to create

      connectorName = newConnectorName;
    } catch (error) {
      showMessage(error.message);
    }
  }

  const update = {
    name: connectorName,
    kind: typeName,
    to: [],
    className,
  };

  updateGraph({ update, dispatcher: { name: 'TOOLBAR' } });
};

export const trimString = string => {
  // Only displays the first 8 digits of the git sha instead so
  // it won't break our layout
  return string.substring(0, 7);
};
