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

import { string, array, object, option } from '../utils/validation';

export const request = () => {
  const group = [string];
  const name = [string];
  const flows = [array];
  const tags = [object, option];

  return {
    name,
    group,
    flows,
    tags,
  };
};

export const response = () => {
  const lastModified = [];
  const objects = [];

  return {
    lastModified,
    objects,
  };
};
