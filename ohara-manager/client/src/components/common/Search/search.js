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
  filter,
  get,
  includes,
  isString,
  isNumber,
  some,
  toLower,
} from 'lodash';

const createSearch = keys => (data, searchText) => {
  return filter(data, obj => {
    return some(keys, key => {
      const value = get(obj, key);
      if (isString(value) || isNumber(value)) {
        return includes(toLower(value), toLower(searchText));
      }
      return false;
    });
  });
};

export { createSearch };
