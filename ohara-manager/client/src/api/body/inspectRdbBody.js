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
  const url = [string];
  const user = [string];
  const workerClusterKey = [object];
  const password = [string];
  const catalogPattern = [string, option];
  const schemaPattern = [string, option];
  const tableName = [string, option];

  return {
    url,
    user,
    workerClusterKey,
    password,
    catalogPattern,
    schemaPattern,
    tableName,
  };
};

export const response = () => {
  const name = [string];
  const tables = [array];

  return {
    name,
    tables,
  };
};
