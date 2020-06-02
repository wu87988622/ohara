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

import { isArray } from 'lodash';

// Return `undefined` means the test has passed!
export const required = (value) =>
  value ? undefined : 'This is a required field';

export const validServiceName = (value) => {
  return /[^0-9a-z]/g.test(value)
    ? 'You only can use lower case letters and numbers'
    : undefined;
};

export const minLength = (min) => (value) => {
  return value.length >= min
    ? undefined
    : `The value must be greater than or equal to ${min} characters long`;
};

export const maxLength = (max) => (value) => {
  return value.length <= max
    ? undefined
    : `The value must be less than or equal to ${max} characters long`;
};

export const minNumber = (min) => (value) => {
  return value >= min
    ? undefined
    : `The value must be greater than or equal to ${min}`;
};

export const maxNumber = (max) => (value) => {
  return value <= max
    ? undefined
    : `The value must be less than or equal to ${max}`;
};

export const validWithRegex = (regex) => (value) => {
  return regex.test(value)
    ? `The value does not meet the rules ${regex}`
    : undefined;
};

export const checkDuplicate = (validateList) => (value) => {
  // we skip this function if the validate list is empty
  if (!validateList || !isArray(validateList)) return undefined;
  return (
    validateList.find((validate) => Object.is(value, validate)) &&
    `The value ${value} is in used`
  );
};

export const composeValidators = (...validators) => (value, allValues, meta) =>
  validators.reduce(
    (error, validator) => error || validator(value, allValues, meta),
    undefined,
  );

export const validWithDef = (def) => (value) => {
  const { necessary, regex, blacklist } = def;
  if (necessary === 'REQUIRED') {
    const requiredValue = required(value);
    if (requiredValue) {
      return requiredValue;
    }
  }
  if (blacklist.length > 0) {
    if (blacklist.find((black) => black === value)) {
      return 'The value is listed in the blacklist and so cannot be used';
    }
  }
  if (regex) {
    const regex = validWithRegex(def.regex);
    const regexValue = regex(value);
    if (regexValue) {
      return regexValue;
    }
  }
  return undefined;
};
