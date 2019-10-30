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
  defaultValue,
  option,
  object,
} from '../utils/validation';

export const request = () => {
  const name = [string];
  const group = [string];
  const brokerClusterKey = {
    name: [string],
    group: [string],
  };
  const numberOfReplications = [number, defaultValue];
  const numberOfPartitions = [number, defaultValue];
  const tags = [object, option];

  return {
    name,
    group,
    brokerClusterKey,
    numberOfReplications,
    numberOfPartitions,
    tags,
  };
};

export const response = () => {
  const lastModified = [];
  const metrics = [];
  const settings = {
    group: [],
    name: [],
    brokerClusterKey: {
      group: [],
      name: [],
    },
    numberOfReplications: [],
    numberOfPartitions: [],
    tags: [],
  };

  return { lastModified, metrics, settings };
};
