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

import { get, isObject } from 'lodash';

import * as connectorApi from 'api/connectorApi';
import { isSource, isSink, isTopic, isStream } from './commonUtils';
import { ICON_MAPS } from 'constants/pipelines';

const getNameByKind = kind => {
  if (isSource(kind)) {
    return 'Source';
  } else if (isSink(kind)) {
    return 'Sink';
  } else if (isTopic(kind) || isStream(kind)) {
    return kind;
  }

  return null;
};

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

export const createConnector = async ({ updateGraph, connector }) => {
  const className = getClassName(connector);
  const connectorKind = getNameByKind(className);
  let connectorName = `Untitled ${connectorKind}`;

  // Default params for creating connectors
  const params = {
    name: connectorName,
    className: className,
    schema: [],
    topics: [],
    numberOfTasks: 1,
    configs: {},
  };

  let id;

  if (isTopic(className)) {
    // Topic was created beforehand, it already has an ID.
    id = connector.id;
    connectorName = connector.name;
  } else if (isStream(className)) {
    id = connector.id;
  } else if (isSource(className)) {
    const res = await connectorApi.createConnector(params);
    id = get(res, 'data.result.id', null);
  } else if (isSink(className)) {
    const res = await connectorApi.createConnector(params);
    id = get(res, 'data.result.id', null);
  }

  const update = {
    icon: ICON_MAPS[className],
    isActive: false,
    name: connectorName,
    kind: className,
    to: [],
    id,
  };

  updateGraph({ update });
};
