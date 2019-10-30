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

import {
  string,
  number,
  array,
  object,
  option,
  generatePort,
  generateName,
} from '../utils/validation';

export const request = () => {
  const name = [string, generateName];
  const group = [string];
  const imageName = [string, option];
  const jmxPort = [number, generatePort];
  const brokerClusterKey = {
    name: [string],
    group: [string],
  };
  const nodeNames = [array];
  const tags = [object, option];
  const jarKey = {
    group: [string],
    name: [string],
  };
  const from = [array];
  const to = [array];

  return {
    name,
    group,
    imageName,
    brokerClusterKey,
    jarKey,
    jmxPort,
    from,
    to,
    nodeNames,
    tags,
  };
};

export const response = () => {
  const aliveNode = [array];
  const lastModified = [number];
  const settings = {
    name: [string],
    group: [string],
    brokerClusterKey: {
      name: [string],
      group: [string],
    },
    imageName: [string],
    jmxPort: [number],
    nodeNames: [array],
    tags: [object],
    from: [],
    to: [],
    jarKey: {
      name: [],
      group: [],
    },
  };
  const metrics = {
    meters: [],
  };

  return {
    aliveNode,
    lastModified,
    settings,
    metrics,
  };
};
