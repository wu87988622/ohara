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

import { CONNECT } from '../../src/constants/urls';

describe('ConnectPage', () => {
  it('creates a new cluster', () => {
    cy.testNodeCheck();

    cy.visit(CONNECT);

    cy.getByText('New cluster').click();

    cy.getByPlaceholderText('cluster00').type('testcluster');
    cy.getByLabelText('Port')
      .click()
      .type('65535');

    cy.getByText('Add node').click();
    cy.get('.ReactModal__Content').should('have.length', 2);

    cy.getByText(Cypress.env('nodeName')).click();
    cy.get('div.ReactModal__Content')
      .eq(1)
      .within(() => {
        cy.getByText('Add').click();
      });
    cy.get('.ReactModal__Content').should('have.length', 1);
    cy.getByText(Cypress.env('nodeName')).should('have.length', 1);

    cy.getByText('Add plugin').click();
    cy.get('.ReactModal__Content').should('have.length', 2);

    cy.uploadPlugin(
      'input[type=file]',
      'plugin/ohara-it-sink.jar',
      'ohara-it-sink.jar',
      'application/java-archive',
    ).wait(500);

    cy.get('div.ReactModal__Content')
      .eq(1)
      .within(() => {
        cy.getByText('Add').click();
      });
    cy.get('.ReactModal__Content').should('have.length', 1);
    cy.getByText('ohara-it-sink.jar').should('have.length', 1);
    cy.get('div.ReactModal__Content')
      .eq(0)
      .within(() => {
        cy.getByText('Add').click();
      });
    cy.get('td').within(() => {
      cy.getByText('testcluster').should('have.length', 1);
    });
  });
});
