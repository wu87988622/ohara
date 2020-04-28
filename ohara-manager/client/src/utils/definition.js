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

import { Type } from 'api/apiInterface/definitionInterface';

export const transformDef = settingDefinitions => {
  const defs = settingDefinitions;
  const imageName = defs.find(def => def.key === 'imageName');
  const isStream = imageName?.defaultValue?.includes('oharastream/stream:');

  const newDefs = {};
  const internalList = [
    'group',
    'tags',
    'zookeeperClusterKey',
    'brokerClusterKey',
    'workerClusterKey',
  ];

  defs.forEach(def => {
    Object.keys(def)
      .filter(key => key.toLowerCase() === 'key')
      .forEach(key => {
        // The value `.` doesn't work very well with final form
        if (def[key].includes('.')) {
          def[key] = def[key].replace(/\./g, '__');
        }

        // Some keys are hidden from our UI
        if (internalList.includes(def[key])) {
          def.internal = true;
        }

        // The value type of the stream's setting definition is BINDING_PORT.
        // We must change it to REMOTE_PORT because We associate it with worker
        // free port in the UI
        if (isStream && def.valueType === Type.BINDING_PORT) {
          def.valueType = Type.REMOTE_PORT;
        }
      });

    newDefs[def.key] = def;
  });

  return Object.values(newDefs);
};
