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

import { pick, sortBy, some, isEqual } from 'lodash';
import { hashByGroupAndName } from './sha';

export const getKey = object => pick(object, ['group', 'name']);

/**
 * @deprecated
 *
 * Please use isEqualByKey(object, other) instead
 */
export const isKeyEqual = (object, other) => isEqualByKey(object, other);

export const isEqualByKey = (object, other) =>
  isEqual(getKey(object), getKey(other));

export const sortByName = objects => sortBy(objects, 'name');

/**
 * @deprecated
 *
 * Please use hash(object) instead
 */
export const hashKey = object => hash(object);

export const hash = object => {
  const key = getKey(object);
  return hashByGroupAndName(key.group, key.name);
};

export const someByKey = (objects, target) =>
  some(objects, obj => isEqual(getKey(obj), getKey(target)));
