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

import { get } from 'lodash';
import { Type } from 'api/apiInterface/definitionInterface';

export const getDefinition = params => {
  const settingDefinitions = get(params, 'settingDefinitions', []);
  const isStream =
    get(
      settingDefinitions.find(def => def.key === 'imageName'),
      'defaultValue',
      '',
    ).indexOf('oharastream/stream:') > -1;
  const newDefs = {};
  const internalList = [
    'group',
    'tags',
    'zookeeperClusterKey',
    'brokerClusterKey',
    'workerClusterKey',
  ];
  settingDefinitions.forEach(def => {
    const keys = Object.keys(def);
    keys
      .filter(key => key === 'key')
      .forEach(key => {
        // The value `.` doesn't work very well with final form
        if (def[key].includes('.')) {
          def[key] = def[key].replace(/\./g, '__');
        }

        // Some keys are hidden from our UI
        if (internalList.includes(def[key])) {
          def.internal = true;
        }

        // The value type of the stream's settingdefinition is BINDING_PORT.
        // We must change it to REMOTE_PORT.
        // Because BINDING_PORT is in the UI, We will associate it with worker free port, but in fact they are not.
        if (isStream && def.valueType === Type.BINDING_PORT) {
          def.valueType = Type.REMOTE_PORT;
        }
      });
    newDefs[def.key] = def;
  });
  return newDefs;
};
