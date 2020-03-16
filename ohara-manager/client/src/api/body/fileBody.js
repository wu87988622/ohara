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

import { object, string, number, array, option } from '../utils/validation';

export const request = () => {
  const file = [object];
  const group = [string];
  const tags = [object, option];

  return { file, group, tags };
};

export const response = () => {
  const name = [string];
  const group = [string];
  const url = [string];
  const lastModified = [number];
  const size = [number];
  const classInfos = [array];
  const tags = [object];

  return {
    name,
    group,
    url,
    lastModified,
    size,
    classInfos,
    tags,
  };
};
