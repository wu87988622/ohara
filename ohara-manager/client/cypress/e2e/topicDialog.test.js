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

describe('Topics Tab in Workspace Settings', () => {
  beforeEach(async () => await deleteAllServices());

  it('topic operations in workspace should be worked normally', () => {
    // create workspace
    cy.createWorkspace();

    cy.contains('button', 'workspace', { matchCase: false })
      .should('exist')
      .click();

    cy.findByText(/^topics$/i)
      .should('exist')
      .click();

    // add shared topics
    const topic1 = generate.serviceName({ length: 5 });
    const topic2 = generate.serviceName({ length: 6 });
    const topic3 = generate.serviceName();
    const topics = [topic1, topic2, topic3];

    topics.forEach(topic => {
      cy.findByText(/^add topic$/i)
        .should('exist')
        .click();
      cy.findByLabelText('topic name', { exact: false }).type(topic);
      cy.findByLabelText('partitions', { exact: false }).type(1);
      cy.findByLabelText('replication factor', { exact: false }).type(1);
      cy.findByText(/^add$/i).click();
    });

    cy.findByText(topic1).should('exist');
    cy.findByText(topic2).should('exist');
    cy.findByText(topic3).should('exist');

    // check the topic view
    cy.findByText(topic3)
      .siblings('td')
      .last()
      .click();

    // check the detail of topic
    cy.contains('td', /^type$/i)
      .siblings('td')
      .contains('Shared')
      .should('exist');
    cy.contains('td', /^state$/i)
      .siblings('td')
      .contains('RUNNING')
      .should('exist');

    cy.findAllByText(/^delete$/i)
      .filter(':visible')
      .should('exist')
      .click();
    cy.contains('span', 'DELETE').click();

    cy.findByText(/^all topics$/i).should('be.visible');
    cy.findByText(topic3).should('not.exist');

    //filter
    cy.findAllByPlaceholderText('Search')
      .filter(':visible')
      .type(topic1);
    cy.findByText(topic1).should('exist');
    cy.findByText(topic2).should('not.exist');

    cy.findAllByPlaceholderText('Search')
      .filter(':visible')
      .clear()
      .type('fake');
    cy.findByText(topic1).should('not.exist');
    cy.findByText(topic2).should('not.exist');
  });
});
