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
  positiveNumber,
  array,
  object,
  boolean,
  option,
  generateValueWithDefaultValue,
} from '../utils/validation';
import { get, isArray, isFunction } from 'lodash';

const necessaryType = {
  required: 'REQUIRED',
  optional: 'OPTIONAL',
  randomValue: 'RANDOM_DEFAULT',
};

export const valueType = {
  boolean: 'BOOLEAN',
  string: 'STRING',
  class: 'CLASS',
  password: 'PASSWORD',
  jdbcTable: 'JDBC_TABLE',
  duration: 'DURATION',
  short: 'SHORT',
  int: 'INT',
  long: 'LONG',
  double: 'DOUBLE',
  remotePort: 'REMOTE_PORT',
  positiveShort: 'POSITIVE_SHORT',
  positiveInt: 'POSITIVE_INT',
  positiveLong: 'POSITIVE_LONG',
  positiveDouble: 'POSITIVE_DOUBLE',
  array: 'ARRAY',
  bindingPort: 'BINDING_PORT',
  objectKey: 'OBJECT_KEY',
  objectKeys: 'OBJECT_KEYS',
  table: 'TABLE',
  tags: 'TAGS',
};

const getTypeWithValueType = key => {
  switch (key) {
    case valueType.boolean:
      return boolean;

    case valueType.string:
    case valueType.class:
    case valueType.password:
    case valueType.jdbcTable:
    case valueType.duration:
      return string;

    case valueType.short:
    case valueType.int:
    case valueType.long:
    case valueType.double:
    case valueType.remotePort:
      return number;

    case valueType.positiveShort:
    case valueType.positiveInt:
    case valueType.positiveLong:
    case valueType.positiveDouble:
      return positiveNumber;

    case valueType.array:
    case valueType.bindingPort:
    case valueType.objectKeys:
      return array;

    case valueType.table:
    case valueType.tags:
      return object;

    case valueType.objectKey:
      return {
        name: [string],
        group: [string],
      };

    default:
      return;
  }
};

export const getDefinition = params => {
  const settingDefinitions = get(params, 'settingDefinitions', []);
  const newDefs = {};
  settingDefinitions.forEach(def => {
    const keys = Object.keys(def);
    keys
      .filter(key => key === 'key' || key === 'group')
      .forEach(key => {
        // The value `.` doesn't work very well with final form
        if (def[key].includes('.')) {
          def[key] = def[key].replace(/\./g, '__');
        }

        // Group is hidden from our UI
        if (def[key] === 'group') {
          def.internal = true;
        }
      });
    newDefs[def.key] = def;
  });
  return newDefs;
};

export const createBody = params => {
  const bodyObj = {};
  Object.keys(params).forEach(key => {
    let body;
    const type = getTypeWithValueType(params[key].valueType);
    if (!isFunction(type)) {
      body = {};
      body = type;
    } else {
      body = [];
      body.push(type);
    }

    if (
      (params[key].necessary === necessaryType.optional ||
        params[key].necessary === necessaryType.randomValue) &&
      isArray(body)
    ) {
      body.push(option);
    }

    if (
      params[key].necessary === necessaryType.optional &&
      params[key].defaultValue &&
      isArray(body)
    ) {
      body.push(generateValueWithDefaultValue(params[key].defaultValue, type));
    }
    bodyObj[key] = body;
  });
  return bodyObj;
};
