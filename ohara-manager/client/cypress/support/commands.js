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

import 'cypress-testing-library/add-commands';

import { setUserKey } from '../../src/utils/authUtils';
import { VALID_USER } from '../../src/constants/cypress';
import * as _ from '../../src/utils/commonUtils';

Cypress.Commands.add('loginWithUi', () => {
  cy.get('[data-testid="username"]').type(VALID_USER.username);
  cy.get('[data-testid="password"]').type(VALID_USER.password);
  cy.get('[data-testid="login-form"]').submit();
});

Cypress.Commands.add('login', () => {
  cy.request({
    method: 'POST',
    url: 'http://localhost:5050/api/login',
    body: {
      username: VALID_USER.username,
      password: VALID_USER.password,
    },
  }).then(res => {
    const token = _.get(res, 'body.token', null);
    if (!_.isNull(token)) {
      setUserKey(token);
    }
  });
});

Cypress.Commands.add('deleteAllNodes', () => {
  Cypress.log({
    name: 'DELETE_ALL_NODES',
  });

  const _ = Cypress._;

  cy.request('GET', 'api/nodes')
    .then(res => res.body)
    .then(nodes => {
      if (!_.isEmpty(nodes)) {
        _.forEach(nodes, node => {
          cy.request('DELETE', `api/nodes/${node.name}`);
        });
      }
    });
});

Cypress.Commands.add('insertNode', node => {
  Cypress.log({
    name: 'INSERT_NODE',
  });

  cy.request('POST', 'api/nodes', {
    name: node.name,
    port: node.port,
    user: node.user,
    password: node.password,
  });
});
