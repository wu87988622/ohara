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
import { isSource, isSink, isTopic, isStream } from './commonUtils';

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

export const createConnector = async ({
  updateGraph,
  connector,
  workerClusterName,
  newConnectorName,
}) => {
  const { typeName } = connector;

  const className = getClassName(connector);
  let connectorName = `Untitled ${typeName}`;
  let id;

  if (isTopic(typeName)) {
    // Topic is created beforehand therefore, an ID is already exist.
    id = connector.id;
    connectorName = connector.name;
  } else if (isStream(typeName)) {
    // stream app needs a jar id in order to create a property form
    const res = await createProperty({
      jarName: connector.jarName,
      name: connectorName,
    });

    id = res.data.result.id;
  } else if (isSource(typeName) || isSink(typeName)) {
    const params = {
      name: newConnectorName,
      'connector.class': className,
      'connector.name': newConnectorName,
      workerClusterName: workerClusterName,
    };
    connectorName = newConnectorName;
    const res = await connectorApi.createConnector(params);
    id = res.data.result.id;
  }

  const update = {
    name: connectorName,
    kind: typeName,
    to: [],
    className,
    id,
  };

  updateGraph({ update });
};

export const trimString = string => {
  // Only displays the first 8 digits of the git sha instead so
  // it won't break our layout
  return string.substring(0, 7);
};
