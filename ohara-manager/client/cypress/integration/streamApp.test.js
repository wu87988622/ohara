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

  it('starts a stream app', () => {
    cy.server();
    cy.route('GET', 'api/pipelines/*').as('getPipeline');
    cy.route('PUT', 'api/pipelines/*').as('putPipeline');
    cy.route('GET', 'api/jars?*').as('getJars');

    cy.createTopic().as('fromTopic');
    cy.createTopic().as('toTopic');

    const pipelineName = makeRandomStr();
    const streamAppName = makeRandomStr();

    cy.visit(URLS.PIPELINES)
      .uploadTestStreamAppJar(Cypress.env('WORKER_NAME'))
      .getByTestId('new-pipeline')
      .click()
      .getByLabelText('Pipeline name')
      .click()
      .type(pipelineName)
      .getByTestId('cluster-select')
      .select(Cypress.env('WORKER_NAME'))
      .getByText('Add')
      .click();

    cy.wait('@getPipeline')
      .getByTestId('toolbar-streams')
      .click()
      .wait('@getJars')
      .getByText('Add')
      .click()
      .getByPlaceholderText('StreamApp name')
      .type(streamAppName)
      .get('.ReactModal__Content')
      .eq(1)
      .within(() => {
        cy.getByText('Add').click();
      });

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

    cy.getByText('streamApp')
      .click({ force: true })
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
      .getByText('streamApp')
      .then($el => {
        cy.wrap($el.parent()).within(() => {
          cy.getByText('Status: running').should('be.exist');
        });
      })
      .getByTestId('stop-button')
      .click();
  });
});
