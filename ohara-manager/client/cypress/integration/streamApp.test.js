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
import { makeRandomStr } from '../utils';

describe('StreamApp', () => {
  before(() => {
    cy.deleteAllWorkers();
    cy.createWorker();
  });

  context('Stream app jars', () => {
    beforeEach(() => {
      cy.server();
      cy.route('GET', 'api/pipelines/*').as('getPipeline');
      cy.route('PUT', 'api/pipelines/*').as('putPipeline');

      const pipelineName = makeRandomStr();

      cy.visit(URLS.PIPELINES)
        .getByTestId('new-pipeline')
        .click()
        .getByLabelText('Pipeline name')
        .click()
        .type(pipelineName)
        .getByTestId('cluster-select')
        .select(Cypress.env('WORKER_NAME'))
        .getByText('Add')
        .click();
    });

    it('adds a streamApp into pipeline graph and removes it with the remove button', () => {
      cy.wait('@getPipeline')
        .uploadStreamAppJar()
        .getByText('Stream app successfully uploaded!')
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
        .should('have.length', 0)
        .deleteStreamApp();
    });

    it('edits a streamApp jar name', () => {
      cy.wait('@getPipeline')
        .uploadStreamAppJar()
        .getByText('ohara-streamapp.jar')
        .click()
        .getByTestId('title-input')
        .type('{leftarrow}{leftarrow}{leftarrow}{leftarrow}') // move the mouse cursor with the left arrow key
        .type('_2{enter}')
        .getByTestId('stream-app-item')
        .find('label')
        .contains('ohara-streamapp_2.jar')
        .deleteStreamApp('ohara-streamapp_2.jar');
    });

    it('deletes streamApp jar', () => {
      cy.wait('@getPipeline')
        .uploadStreamAppJar()
        .getByTestId('delete-stream-app')
        .click()
        .getByText('Yes, Delete this jar')
        .click()
        .wait(500)
        .queryAllByTestId('stream-app-item')
        .should('have.length', 0);
    });
  });

  context('Stream app property form', () => {
    beforeEach(() => {
      cy.server();
      cy.route('GET', 'api/pipelines/*').as('getPipeline');
      cy.route('PUT', 'api/pipelines/*').as('putPipeline');

      cy.createTopic().as('fromTopic');
      cy.createTopic().as('toTopic');

      const pipelineName = makeRandomStr();

      cy.visit(URLS.PIPELINES)
        .getByTestId('new-pipeline')
        .click()
        .getByLabelText('Pipeline name')
        .click()
        .type(pipelineName)
        .getByTestId('cluster-select')
        .select(Cypress.env('WORKER_NAME'))
        .getByText('Add')
        .click();
    });

    // TODO: remove the pipeline once the test is finished
    it('starts a stream app', () => {
      cy.wait('@getPipeline')
        .uploadStreamAppJar()
        .getByText('Add')
        .click();

      // TODO: these two topics can be added via API which should be faster than
      // adding from the UI
      cy.getByTestId('toolbar-topics')
        .click({ force: true })
        .get('@fromTopic')
        .then(from => {
          cy.getByTestId('topic-select').select(from.name);
        })
        .getByText('Add')
        .click()
        .wait('@putPipeline');

      cy.getByTestId('toolbar-topics')
        .click({ force: true })
        .get('@toTopic')
        .then(to => {
          cy.getByTestId('topic-select').select(to.name);
        })
        .getByText('Add')
        .click()
        .wait('@putPipeline');

      cy.getByText('Untitled streamApp')
        .click()
        .getByDisplayValue('select a from topic...')
        .get('@fromTopic')
        .then(from => {
          cy.getByDisplayValue('select a from topic...')
            .select(from.name)
            .wait(2000) // UI has one sec throttle, so we need to wait a bit time and then wait for the request
            .wait('@putPipeline');
        })
        .getByDisplayValue('select a to topic...')
        .get('@toTopic')
        .then(to => {
          cy.getByDisplayValue('select a to topic...')
            .select(to.name)
            .wait(2000)
            .wait('@putPipeline');
        })
        .getByTestId('start-button')
        .click()
        .getByText('Stream app successfully started!')
        .should('be.exist')
        .getByText('Untitled streamApp')
        .then($el => {
          cy.wrap($el.parent()).within(() => {
            cy.getByText('Status: running').should('be.exist');
          });
        })
        .getByTestId('stop-button')
        .click();
    });
  });
});
