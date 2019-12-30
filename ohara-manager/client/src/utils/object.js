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

import { find, get, has, pick, sortBy } from 'lodash';
import { hashByGroupAndName } from './sha';

export const checkObject = object => {
  if (!has(object, 'group' || !has(object, 'name')))
    throw new Error('The object must consist of name and group.');
};

export const checkKey = key => {
  if (!has(key, 'group' || !has(key, 'name')))
    throw new Error(
      'The key must consist of name and group. ex: { name: "", group: "" }',
    );
};

export const getKey = object => pick(object, ['group', 'name']);

export const isKeyEqual = (object, other) =>
  get(object, 'name') === get(other, 'name') &&
  get(object, 'group') === get(other, 'group');

export const sortByName = objects => sortBy(objects, 'name');

export const hashKey = object => {
  if (!has(object, 'group' || !has(object, 'name')))
    throw new Error(
      'To calculate the hash key, the group and name must be provided.',
    );
  return hashByGroupAndName(get(object, 'group'), get(object, 'name'));
};

export const findByGroupAndName = (objects, group, name) =>
  find(objects, o => get(o, 'group') === group && get(o, 'name') === name);
