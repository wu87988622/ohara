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
  const name = Cypress.env('nodeHost');
  const port = Cypress.env('nodePort');
  const user = Cypress.env('nodeUser');
  const password = Cypress.env('nodePass');
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

export const recursiveDeleteWorker = (endPoint, serviceName) => {
  // We need to wait for specific service removed from the node
  // then we can move on and remove another service
  cy.request('GET', endPoint).then(res => {
    const isServiceExit = res.body.some(
      service => service.name === serviceName,
    );

    // Target service is not in the list anymore, break the loop
    if (!isServiceExit) return;

    // Wait and make another request
    cy.wait(1500);
    recursiveDeleteWorker(endPoint, serviceName);
  });
};

export const randomName = () => {
  var text = '';
  var possible = 'abcdefghijklmnopqrstuvwxyz0123456789';

  for (var i = 0; i < 5; i++)
    text += possible.charAt(Math.floor(Math.random() * possible.length));

  return text;
};
