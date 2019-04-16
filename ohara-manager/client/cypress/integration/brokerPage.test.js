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

import { BROKER } from '../../src/constants/urls';

describe('BrokerPage', () => {
  beforeEach(() => cy.visit(BROKER));

  it('has correct page heading', () => {
    cy.getByText('Services > Broker').should('have.length', 1);
  });

  it('displays broker node info in the list', () => {
    const brokerList = Cypress.env('node_name');
    cy.request('GET', 'api/brokers')
      .then(res => res.body[0].clientPort) // we now only have one broker cluster!
      .as('clientPort');

    cy.get('@clientPort').then(port => {
      const expected = `${brokerList}:${port}`;
      cy.getByLabelText('Broker list').should('have.value', expected);
    });
  });

  it('creates a new topic', () => {
    const topicName = 'test topic';

    cy.visit(BROKER)
      .getByText('New topic')
      .click()
      .getByLabelText('Topic name')
      .click()
      .type(topicName)
      .getByLabelText('Partitions')
      .click()
      .type('1')
      .getByLabelText('Replication factor')
      .click()
      .type('1')
      .getByText('Save')
      .click()
      .getByText('Topic successfully created!')
      .should('have.length', 1)
      .getByText(topicName)
      .should('have.length', 1)
      .deleteTopic(topicName);
  });
});
