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

import { deleteAllServices } from '../utils';
import * as generate from '../../src/utils/generate';

const nodeHost = Cypress.env('nodeHost');

describe('Workspace Settings', () => {
  before(() => deleteAllServices());

  it('topics operations of Settings', () => {
    cy.createWorkspace({});

    const sharedTopicName = generate.serviceName({ prefix: 'topic' });
    // click the settings dialog
    cy.findByText('workspace1').click();
    cy.contains('li', 'Settings').click();
    // click the topics button
    cy.findAllByText(/^topics in this workspace$/i).click({ force: true });
    // create topic
    cy.findByTitle('Create Topic')
      .should('be.enabled')
      .click();
    cy.findAllByLabelText('Topic name', { exact: false })
      .filter(':visible')
      .type(sharedTopicName);
    cy.findAllByLabelText('Partitions', { exact: false })
      .filter(':visible')
      .type('1');
    cy.findAllByLabelText('Replication factor', { exact: false })
      .filter(':visible')
      .type('1');
    cy.findAllByText(/^create$/i)
      .filter(':visible')
      .click();
    // the new added topic should exist
    cy.findByText(sharedTopicName).should('exist');
    cy.contains('td', 'RUNNING').should('exist');

    // assert the topic view
    cy.findAllByTitle('View topic')
      .filter(':visible')
      .first()
      .click();
    cy.contains('td', /^state$/i)
      .siblings('td')
      .contains('RUNNING')
      .should('exist')
      // press "ESC" again back to topic list
      .trigger('keydown', { keyCode: 27, which: 27 });

    // the shared topic should be OK to be removed
    cy.findAllByTitle('Delete topic')
      .filter(':visible')
      .first()
      .should('not.be.disabled')
      .click();
    cy.findAllByTestId('confirm-button-DELETE')
      .filter(':visible')
      .first()
      .click();

    // after remove, the topic should not exist
    cy.findAllByText(sharedTopicName).should('not.exist');
  });

  it('zookeeper operations of Settings', () => {
    cy.visit('/');

    // click the settings dialog
    cy.findByText('workspace1').click();
    cy.contains('li', 'Settings').click();
    // click the zookeeper button
    cy.findAllByText(/^zookeeper nodes$/i).click({ force: true });

    cy.findAllByText(nodeHost)
      .should('exist')
      .siblings('td')
      .contains('Available')
      .should('exist');
  });

  it('broker operations of Settings', () => {
    cy.visit('/');

    // click the settings dialog
    cy.findByText('workspace1').click();
    cy.contains('li', 'Settings').click();
    // click the zookeeper button
    cy.findAllByText(/^broker nodes$/i).click({ force: true });

    cy.findAllByText(nodeHost)
      .should('exist')
      .siblings('td')
      .contains('Available')
      .should('exist');
  });

  it('worker operations of Settings', () => {
    cy.visit('/');

    // click the settings dialog
    cy.findByText('workspace1').click();
    cy.contains('li', 'Settings').click();
    // click the zookeeper button
    cy.findAllByText(/^worker nodes$/i).click({ force: true });

    cy.findAllByText(nodeHost)
      .should('exist')
      .siblings('td')
      .contains('Available')
      .should('exist');
  });

  it('nodes operations of Settings', () => {
    cy.visit('/');

    // click the settings dialog
    cy.findByText('workspace1').click();
    cy.contains('li', 'Settings').click();
    // click the zookeeper button
    cy.findAllByText(/^workspace nodes$/i).click({ force: true });

    cy.findAllByText(nodeHost)
      .should('exist')
      .siblings('td')
      .contains('Available')
      .should('exist');
  });
});
