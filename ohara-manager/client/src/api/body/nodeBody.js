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

import { string, number, array, option } from '../utils/validation';

export const request = () => {
  const hostname = [string];
  const port = [number, option];
  const user = [string, option];
  const password = [string, option];
  const tags = [array, option];

  return { hostname, port, user, password, tags };
};

export const response = () => {
  const hostname = [string];
  const port = [number];
  const user = [string];
  const password = [string];
  const lastModified = [number];
  const tags = [array];
  const services = {
    name: [string],
    clusterKeys: [
      {
        group: [string],
        name: [string],
      },
    ],
  };
  const state = [string];
  const resources = [array];
  return {
    hostname,
    port,
    user,
    password,
    lastModified,
    tags,
    services,
    state,
    resources,
  };
};
