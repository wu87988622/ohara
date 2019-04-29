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

import { WORKER } from '../../src/constants/urls';

describe('WorkerPage', () => {
  before(() => {
    cy.deleteWorker();
  });

  it('creates a new cluster', () => {
    const nodeName = Cypress.env('node_name');
    const clusterName = 'testcluster';

    cy.registerWorker(clusterName);

    cy.visit(WORKER)
      .getByText('New cluster')
      .click()
      .getByPlaceholderText('cluster00')
      .type(clusterName)
      .getByLabelText('Port')
      .click()
      .type('65535')
      .getByText('Add node')
      .click()
      .getByText(nodeName)
      .click();

    cy.get('.ReactModal__Content')
      .eq(1)
      .within(() => {
        cy.getByText('Add').click();
      })
      .getByText(nodeName)
      .should('have.length', 1)
      .getByText('Add plugin')
      .click();

    cy.uploadJar(
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
      cy.getByText(clusterName).should('have.length', 1);
    });
  });
});
