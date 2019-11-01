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
  boolean,
  option,
  generateValueWithDefaultValue,
} from '../utils/validation';
import { isString, isArray, isFunction } from 'lodash';

const getTypeWithValueType = key => {
  switch (key) {
    case 'BOOLEAN':
      return boolean;

    case 'STRING':
    case 'CLASS':
    case 'PASSWORD':
    case 'JDBC_TABLE':
    case 'DURATION':
      return string;

    case 'SHORT':
    case 'INT':
    case 'LONG':
    case 'DOUBLE':
    case 'PORT':
      return number;

    case 'ARRAY':
    case 'BINDING_PORT':
    case 'OBJECT_KEYS':
      return array;

    case 'TABLE':
    case 'TAGS':
      return object;

    case 'OBJECT_KEY':
      return {
        name: [string],
        group: [string],
      };

    default:
      return;
  }
};

export const getConnect = params => {
  const connectorDefinition = params.allConnectorDefinitions
    .filter(param => param.state === 'RUNNING')
    .map(param => param.connectorDefinitions)
    .flatMap(param => param)
    .filter(param => param.className === params.className);
  return getCluster(connectorDefinition[0]);
};

export const getCluster = params => {
  const { settingDefinitions } = params;
  const definitionsObj = {};
  settingDefinitions.forEach(definition => {
    const obj = definition;
    const keys = Object.keys(obj);
    keys.forEach(key => {
      if (!isString(obj[key])) return;
      if (obj[key].indexOf('.') !== -1) {
        obj[key] = obj[key].replace(/\./g, '__');
      }
    });
    definitionsObj[obj.key] = obj;
  });
  return definitionsObj;
};

export const getTopic = params => {
  const topic = params.filter(param => param.state === 'RUNNING')[0];
  const { topicDefinition } = topic;
  return getCluster(topicDefinition);
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
    if (!params[key].required && isArray(body)) {
      body.push(option);
    }

    if (params[key].defaultValue && params[key].editable && isArray(body)) {
      //TODO : Temporary modification waiting for backend repair #3162
      body.push(generateValueWithDefaultValue(params[key].defaultValue, type));
    }
    bodyObj[key] = body;
  });
  return bodyObj;
};
