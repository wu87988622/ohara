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

import { isString } from 'lodash';

export const getConnect = params => {
  const { connectors } = params;
  const connectorObj = {};
  connectors.forEach(connect => {
    const name = connect.className.split('.').pop();
    connect.definitions.map(definition => {
      const obj = definition;
      const keys = Object.keys(obj);
      keys.forEach(key => {
        if (!isString(obj[key])) return;
        if (obj[key].indexOf('.') !== -1) {
          obj[key] = obj[key].replace(/\./g, '_');
        }
      });
      return obj;
    });
    connectorObj[name] = connect;
  });
  return connectorObj;
};
