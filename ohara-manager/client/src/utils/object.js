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

import { find, get, has, sortBy } from 'lodash';
import { hashByGroupAndName } from './sha';

export const getKey = object => {
  return {
    name: get(object, 'settings.name'),
    group: get(object, 'settings.group'),
  };
};

export const isKeyEqual = (object, other) =>
  get(object, 'settings.name') === get(other, 'settings.name') &&
  get(object, 'settings.group') === get(other, 'settings.group');

export const sortByName = objects => sortBy(objects, 'settings.name');

export const hashKey = object => {
  if (!has(object, 'settings.group' || !has(object, 'settings.name')))
    throw new Error(
      'To calculate the hash key, the settings.group and settings.name must be provided.',
    );
  return hashByGroupAndName(
    get(object, 'settings.group'),
    get(object, 'settings.name'),
  );
};

export const findByGroupAndName = (objects, group, name) =>
  find(
    objects,
    o => get(o, 'settings.group') === group && get(o, 'settings.name') === name,
  );
