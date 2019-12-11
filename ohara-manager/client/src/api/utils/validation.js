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
import * as generate from '../../utils/generate';

// we should handle a null value from API which is string type
export const string = value => isString(value) || value == null;

export const number = value => isNumber(value);

export const positiveNumber = value => isNumber(value) && value > 0;

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

    case boolean:
      return false;

    default:
      return;
  }
};

const converterBooleanWithString = string => {
  if (isBoolean(string)) return string;
  const lowerCaseString = string.toLowerCase();
  switch (lowerCaseString) {
    case 'true':
      return true;

    case 'false':
      return false;

    default:
      return;
  }
};

export const converterValueWithDefaultValue = (value, type) => {
  switch (type) {
    case string:
      return value;

    case number:
      return Number(value);

    case array:
      //TODO : The conversion mode that has not been determined is not handled at present
      return value;

    case object:
      return JSON.parse(value);

    case boolean:
      return converterBooleanWithString(value);

    default:
      return;
  }
};

export const generateValueWithDefaultValue = (value, type) => {
  return () => {
    return converterValueWithDefaultValue(value, type);
  };
};

export const getType = params => {
  if (object(params)) {
    params = [];
  }
  const type = params.filter(param =>
    [string, number, array, positiveNumber, object, boolean].includes(param),
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
