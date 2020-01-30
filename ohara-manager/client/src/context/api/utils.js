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

import { has, isEmpty, keys, pick } from 'lodash';

import ContextApiError from 'context/ContextApiError';
import { getDefinition } from 'api/utils/definitionsUtils';

export const validate = values => {
  if (!has(values, 'name'))
    throw new ContextApiError({
      title: 'The values must contain the name property',
    });
};

// for workspace stage object, we use a "prefix" to identify it
export const stageGroup = group => 'stage'.concat(group);

export const generateClusterResponse = params => {
  const { values = {}, stageValues = {}, inspectInfo = {} } = params;

  if (isEmpty(inspectInfo)) {
    return {
      ...values,
      // Flatten settings, remove them when appropriate
      settings: values,
      stagingSettings: stageValues,
      ...inspectInfo,
    };
  }
  const definitions = getDefinition(inspectInfo);
  const definitionKeys = keys(definitions);
  // Flatten settings, remove them when appropriate
  const settings = pick(values, definitionKeys);
  const stagingSettings = pick(stageValues, definitionKeys);
  return { ...values, settings, stagingSettings, ...inspectInfo };
};
