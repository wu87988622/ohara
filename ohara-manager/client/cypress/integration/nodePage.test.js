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

import * as URLS from '../../src/constants/urls';
import * as MESSAGES from '../../src/constants/messages';

// We cannot test the nodes page right now as
// nodes are created prior to all tests and we only have one node info
describe.skip('NodePage', () => {
  const nodeListExpectedToEq = length => {
    cy.getByTestId('node-list')
      .get('tbody > tr')
      .should('have.length', length);
  };

  const getTestData = () => {
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

  const getFakeData = () => ({
    name: Cypress.env('node_name') || 'node00',
    port: 21,
    user: 'user',
    password: 'password',
  });

  beforeEach(() => {
    cy.visit(URLS.NODES);
  });

  it('creates a node and displays in the node list page', () => {
    cy.visit(URLS.NODES);

    const testData = getTestData();
    if (testData) {
      cy.getByText('New node').click();
      cy.getByTestId('name-input')
        .type(testData.name)
        .blur()
        .getByTestId('port-input')
        .type(testData.port)
        .blur()
        .getByTestId('user-input')
        .type(testData.user)
        .blur()
        .getByTestId('password-input')
        .type(testData.password)
        .blur();

      cy.getByText('Test connection').click();
      cy.get('#toast-container > .toast-success').contains(
        MESSAGES.TEST_SUCCESS,
      );
      cy.getByText('Save').click();
      nodeListExpectedToEq(1);
    }
  });

  it('displays an error message when the test connection failed', () => {
    cy.getByText('New node').click();
    cy.getByText('Test connection').click();
    cy.get('#toast-container > .toast-error').should('be.visible');
  });
  it('updates node', () => {
    const fakeData = getFakeData();
    cy.insertNode(fakeData);
    cy.visit(URLS.NODES);
    cy.getByTestId('edit-node-icon').click();
    cy.getByTestId('name-input').should('have.value', fakeData.name);

    const testData = getTestData();
    if (testData) {
      cy.getByTestId('port-input')
        .clear()
        .type(testData.port)
        .blur()
        .getByTestId('user-input')
        .clear()
        .type(testData.user)
        .blur()
        .getByTestId('password-input')
        .clear()
        .type(testData.password)
        .blur();

      cy.getByText('Test connection')
        .click()
        .getByText('Save')
        .click();

      cy.getByTestId('node-item').contains(
        `user: ${testData.user}, port: ${testData.port}`,
      );
    }
  });
});
