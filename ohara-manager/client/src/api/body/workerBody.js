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
  generateName,
  generatePort,
  defaultValue,
} from '../utils/validation';

// convert the request parameter to another key
// ex: { node: 'nodeNames' }
export const reqConverter = {};

// convert the response parameter to another key
// ex: { 'settings.name': 'name' }
export const resConverter = {};

export const request = () => {
  const name = [string, generateName];
  const group = [string, option];
  const clientPort = [number, generatePort];
  const jmxPort = [number, generatePort];
  const freePorts = [array, generatePort];
  const brokerClusterKey = {
    name: [string],
    group: [string, generateName],
  };
  const groupId = [string, generateName];
  const configTopicName = [string, generateName];
  const configTopicReplications = [number, defaultValue];
  const offsetTopicName = [string, generateName];
  const offsetTopicReplications = [number, defaultValue];
  const offsetTopicPartitions = [number, defaultValue];
  const statusTopicName = [string, generateName];
  const statusTopicReplications = [number, defaultValue];
  const statusTopicPartitions = [number, defaultValue];
  const nodeNames = [array];
  const tags = [object, option];
  const jarKeys = [array, defaultValue];
  return {
    name,
    group,
    clientPort,
    jmxPort,
    freePorts,
    brokerClusterKey,
    groupId,
    configTopicName,
    configTopicReplications,
    offsetTopicName,
    offsetTopicReplications,
    offsetTopicPartitions,
    statusTopicName,
    statusTopicReplications,
    statusTopicPartitions,
    nodeNames,
    tags,
    jarKeys,
  };
};

export const response = () => {
  const aliveNodes = [array];
  const state = [string];
  const error = [string];
  const lastModified = [number];
  const connectors = [array];
  const settings = {
    statusTopicName: [string],
    name: [string],
    group: [string],
    offsetTopicPartitions: [number],
    brokerClusterKey: {
      group: [string],
      name: [string],
    },
  };
  const tags = [object];
  const jarInfos = [array];
  const offsetTopicName = [string];
  const groupId = [string];
  const statusTopicReplications = [number];
  const offsetTopicReplications = [number];
  const configTopicReplications = [number];
  const statusTopicPartitions = [number];
  const configTopicName = [string];
  const jmxPort = [number];
  const clientPort = [number];
  const freePorts = [array];
  const jarKeys = [array];
  const nodeNames = [array];

  return [
    aliveNodes,
    state,
    error,
    lastModified,
    connectors,
    settings,
    tags,
    jarInfos,
    offsetTopicName,
    groupId,
    statusTopicReplications,
    offsetTopicReplications,
    configTopicReplications,
    statusTopicReplications,
    statusTopicPartitions,
    configTopicName,
    jmxPort,
    clientPort,
    freePorts,
    jarKeys,
    nodeNames,
  ];
};
