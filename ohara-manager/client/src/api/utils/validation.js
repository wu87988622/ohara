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
  isString,
  isNumber,
  isArray,
  isObject,
  isBoolean,
  isFunction,
} from 'lodash';
import * as generate from './generate';

export const string = value => isString(value);

export const number = value => isNumber(value);

export const array = value => isArray(value);

export const object = value => {
  return isObject(value) && !array(value);
};

export const boolean = value => isBoolean(value);

export const generateName = (params, length = 5) => {
  const type = getType(params);
  switch (type) {
    case string:
      return generate.serviceName({ length });

    case array:
      return [generate.serviceName({ length })];

    default:
      return;
  }
};

export const generatePort = params => {
  const type = getType(params);
  switch (type) {
    case number:
      return generate.port();

    case array:
      return [generate.port()];

    default:
      return;
  }
};

export const defaultValue = params => {
  const type = getType(params);
  switch (type) {
    case string:
      return '';

    case number:
      return 1;

    case array:
      return [];

    case object:
      return {};

    default:
      return;
  }
};

export const generateValueWithDefaultValue = (value, type) => {
  return get => {
    //TODO : Temporary modification waiting for backend repair #3162
    return type === number && string(value) ? Number(value) : value;
  };
};

export const getType = params => {
  if (object(params)) {
    params = [];
  }
  const type = params.filter(param =>
    [string, number, array, object, boolean].includes(param),
  )[0];
  if (type) return type;
  return object;
};

export const getGenerate = params => {
  const defaultGenerate = params.filter(param =>
    [generateName, generatePort, defaultValue].includes(param),
  )[0];

  if (defaultGenerate) return defaultGenerate;
  return params
    .filter(param => isFunction(param))
    .filter(param => param.name === generateValueWithDefaultValue().name)[0];
};
export const option = 'option';
