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

describe.skip('StreamApp', () => {
  before(() => {
    cy.deleteAllWorkers();
    cy.createWorker();
  });

  beforeEach(() => {
    cy.server();
    cy.route('GET', 'api/pipelines/*').as('getPipeline');

    cy.visit(URLS.PIPELINE)
      .getByTestId('new-pipeline')
      .click()
      .getByTestId('cluster-select')
      .select(Cypress.env('WORKER_NAME'))
      .getByText('Next')
      .click();
  });

  it('adds a streamApp into pipeline graph and removes it with the remove button', () => {
    cy.wait('@getPipeline')
      .getByTestId('toolbar-streams')
      .click()
      .uploadJar(
        'input[type=file]',
        'streamApp/ohara-streamapp.jar',
        'ohara-streamapp.jar',
        'application/java-archive',
      )
      .wait(500);

    cy.getByText('Stream app successfully uploaded!')
      .should('have.length', 1)
      .getByText('ohara-streamapp.jar')
      .getByText('Add')
      .click()
      .getByText('Untitled streamApp')
      .should('be.exist')
      .click()
      .getByTestId('delete-button')
      .click()
      .getByText('Yes, Remove this stream app')
      .click()
      .wait('@getPipeline')
      .queryAllByText('Untitled streamApp')
      .should('have.length', 0);
  });

  it.skip('edits streamApp name', () => {
    cy.wait('@getPipeline')
      .getByTestId('toolbar-streams')
      .click()
      .get('input[type=file]')
      .uploadJar(
        'input[type=file]',
        'streamApp/ohara-streamapp.jar',
        'ohara-streamapp.jar',
        'application/java-archive',
      )
      .wait(500)
      .getByText('ohara-streamapp.jar')
      .click()
      .getByTestId('title-input')
      .type('{leftarrow}{leftarrow}{leftarrow}{leftarrow}')
      .type('_2{enter}')
      .getByTestId('stream-app-item')
      .find('label')
      .contains('streamApp_2.jar');
  });

  it.skip('deletes streamApp', () => {
    cy.wait('@getPipeline')
      .getByTestId('toolbar-streams')
      .click()
      .get('input[type=file]')
      .uploadJar(
        'input[type=file]',
        'streamApp/ohara-streamapp.jar',
        'ohara-streamapp.jar',
        'application/java-archive',
      )
      .wait(500)
      .getByTestId('delete-stream-app')
      .click()
      .getByText('Yes, Delete this row')
      .click()
      .wait(500)
      .queryAllByTestId('stream-app-item')
      .should('have.length', 0);
  });
});
