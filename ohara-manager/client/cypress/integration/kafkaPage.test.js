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

import { KAFKA } from '../../src/constants/urls';

// TODO: skip these tests for now since this will hit the real APIs
// Need to start the Configurator when running on CI
describe('KafkaPage', () => {
  it('creates a new topic', () => {
    cy.visit(KAFKA);

    cy.getByText('New topic').click();

    cy.get('.ReactModal__Content').should('have.length', 1);

    cy.getByLabelText('Topic name').type('test cluster');
    cy.getByLabelText('Partitions').type('1');
    cy.getByLabelText('Replication factor').type('2');
    cy.getByText('Save').click();

    cy.get('.toast-success').should('have.length', 1);
    cy.get('.ReactModal__Content').should('have.length', 0);
    cy.get('td')
      .contains('test cluster')
      .should('have.length', 1);
  });
});
