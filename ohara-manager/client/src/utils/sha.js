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

import sha from 'sha.js';
import { join, isNil } from 'lodash';

const defaultHashOptions = {
  algorithm: 'sha256',
  encode: 'hex',
  maxLength: 8,
};

// private function
const hash = (input = '', options = defaultHashOptions) =>
  sha(options.algorithm)
    .update(input)
    .digest(options.encode)
    .substring(0, options.maxLength + 1);
const hashWith = (...args) => hash(join(args, ''));

/**
 * hash the group + name
 * @param {*} group object group
 * @param {*} name object name
 */
export const hashByGroupAndName = (group, name) => {
  if (isNil(group) || isNil(name)) {
    throw new Error(
      `The group or name must not be empty, actual: {${group}, ${name}}`,
    );
  }
  hashWith(group, name);
};
