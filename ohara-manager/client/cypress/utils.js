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

export const makeRandomPort = () => {
  return Math.floor(Math.random() * 65535) + 1;
};

export const getFakeNode = () => {
  const name = Cypress.env('node_name');
  const port = Cypress.env('node_port');
  const user = Cypress.env('node_user');
  const password = Cypress.env('node_password');
  if (!name) return null;
  return {
    name,
    port,
    user,
    password,
  };
};

export const makeRandomStr = prefix => {
  const random = Math.random()
    .toString(36)
    .substring(7);

  return prefix ? `${prefix}${random}` : random;
};
