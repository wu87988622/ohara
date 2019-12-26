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

import { forEach, has, map, isEmpty } from 'lodash';

import { getDefinition } from 'api/utils/definitionsUtils';

export const validate = values => {
  if (!has(values, 'name'))
    throw new Error('The values must contain the name property');
};

// for workspace stage object, we use a "prefix" to identify it
export const stageGroup = group => 'stage'.concat(group);

export const generateClusterResponse = ({
  values = {},
  stageValues = {},
  inspectInfo = {},
} = {}) => {
  let result = {
    settings: {},
    stagingSettings: {},
    settingDefinitions: [],
  };
  if (!isEmpty(inspectInfo)) {
    const definitions = getDefinition(inspectInfo);
    const definitionKeys = Object.keys(definitions);
    if (!isEmpty(values)) {
      forEach(values, (value, key) => {
        definitionKeys.find(d => d === key)
          ? (result.settings[key] = value)
          : (result[key] = value);
      });
    }
    if (!isEmpty(stageValues)) {
      forEach(stageValues, (value, key) => {
        if (definitionKeys.find(d => d === key))
          result.stagingSettings[key] = value;
      });
    }
    result.settingDefinitions = map(definitions, value => value);
  } else {
    if (!isEmpty(values)) result.settings = values;
    if (!isEmpty(stageValues)) result.stagingSettings = stageValues;
  }

  if (!isEmpty(inspectInfo.imageName)) result.imageName = inspectInfo.imageName;
  if (!isEmpty(inspectInfo.classInfos))
    result.classInfos = inspectInfo.classInfos;

  return result;
};
