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

import { WORKSPACES } from '../../src/constants/urls';
import * as utils from '../utils';

describe('WorkspacesPage', () => {
  before(() => {
    cy.deleteAllWorkers();
    cy.createWorker();
  });

  beforeEach(() => {
    cy.server();
    cy.route('GET', 'api/workers').as('getWorkers');
    cy.route('GET', 'api/topics').as('getTopics');
  });

  it('creates a new connect worker cluster', () => {
    const nodeName = Cypress.env('nodeHost');
    const clusterName = utils.makeRandomStr();
    const port = utils.makeRandomPort();

    cy.registerWorker(clusterName);

    cy.visit(WORKSPACES)
      .getByText('New workspace')
      .click()
      .getByPlaceholderText('cluster00')
      .type(clusterName)
      .getByLabelText('Port')
      .click()
      .type(port)
      .getByText('Add node')
      .click();

    cy.get('.ReactModal__Content')
      .eq(1)
      .within(() => {
        cy.getByText(nodeName)
          .click()
          .getByText('Add')
          .click();
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
    cy.getByText('ohara-it-sink').should('have.length', 1);
    cy.get('div.ReactModal__Content')
      .eq(0)
      .within(() => {
        cy.getByText('Add').click();
      });

    cy.getByText(clusterName).should('have.length', 1);
  });

  it('adds a new topic', () => {
    const topicName = utils.makeRandomStr();

    cy.visit(WORKSPACES)
      .wait('@getWorkers')
      .getByTestId(Cypress.env('WORKER_NAME'))
      .click()
      .getByText('Topics')
      .click()
      .getByText('New topic')
      .click()
      .getByPlaceholderText('Kafka Topic')
      .type(topicName)
      .getByPlaceholderText('1')
      .type(1)
      .getByPlaceholderText('3')
      .type(1)
      .getByText('Save')
      .click()
      .wait('@getTopics')
      .getByText(topicName)
      .should('have.length', 1);
  });

  it('deletes a topic', () => {
    cy.createTopic().as('newTopic');
    cy.visit(WORKSPACES)
      .wait('@getWorkers')
      .getByTestId(Cypress.env('WORKER_NAME'))
      .click()
      .getByText('Topics')
      .click()
      .wait('@getTopics')
      .get('@newTopic')
      .then(topic => {
        cy.getByTestId(topic.name)
          .click({ force: true })
          .getByText('Delete')
          .click()
          .getByText(`Successfully deleted the topic: ${topic.name}`)
          .should('have.length', 1);
      });
  });
});
