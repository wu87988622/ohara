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

import { get, includes, isBoolean, isNumber, isString, replace } from 'lodash';

const getAllowedKeys = definitions =>
  definitions
    .filter(def => def.group !== 'core')
    .filter(def => !def.internal)
    .filter(def => def.permission === 'EDITABLE')
    .sort((def, other) => def.orderInGroup >= other.orderInGroup)
    .map(def => def.key);

export const toAutofillData = (formValues = {}, definitions) => {
  const keys = getAllowedKeys(definitions);
  return {
    settings: keys
      .map(key => ({
        key: replace(key, /__/g, '.'), // Like "ftp__hostname" becomes "ftp.hostname"
        value: formValues[key],
      }))
      .filter(({ value }) => {
        return isBoolean(value) || isNumber(value) || isString(value);
      }),
  };
};

export const toFormValues = (autofillData, definitions) => {
  const keys = getAllowedKeys(definitions);
  return get(autofillData, 'settings', [])
    .map(setting => ({
      key: replace(setting.key, /\./g, '__'), // Like "ftp.hostname" becomes "ftp__hostname"
      value: setting.value,
    }))
    .filter(setting => includes(keys, setting.key))
    .reduce((acc, cur) => {
      const { key, value } = cur;
      acc[key] = value;
      return acc;
    }, {});
};
