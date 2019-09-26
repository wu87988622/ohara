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
import * as generate from '../../src/utils/generate';

describe.skip('StreamApp', () => {
  before(() => {
    cy.removeWorkers();
    cy.addWorker();
  });

  beforeEach(() => {
    cy.server();
    cy.route('GET', 'api/pipelines/*').as('getPipeline');
    cy.route('PUT', 'api/pipelines/*').as('putPipeline');
    cy.route('GET', 'api/files?*').as('getJars');
  });

  it('starts a stream app', () => {
    const fromTopicName = generate.serviceName({ prefix: 'topic' });
    const toTopicName = generate.serviceName({ prefix: 'topic' });
    const pipelineName = generate.serviceName({
      prefix: 'pipeline',
      length: 5,
    });
    const streamAppName = generate.serviceName({ prefix: 'stream' });

    cy.addTopic(fromTopicName);
    cy.addTopic(toTopicName);

    cy.visit(URLS.PIPELINES)
      .uploadTestStreamAppJar(Cypress.env('WORKER_NAME'))
      .findByText('NEW PIPELINE')
      .click()
      .findByTestId('pipeline-name-input')
      .type(pipelineName)
      .findByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('WORKER_NAME')}]`)
      .click()
      .findByText('ADD')
      .click()
      .wait('@getPipeline');

    cy.findByTestId('toolbar-streams')
      .click()
      .wait('@getJars')
<<<<<<< HEAD
      .getByText('Please select...')
      .click()
      .get(`li[data-value='ohara-streamapp.jar']`)
      .click()
      .getByText('ADD')
      .click()
      .getByPlaceholderText('mystreamapp')
=======
      .findByText('Add')
      .click()
      .findByPlaceholderText('StreamApp name')
>>>>>>> wip
      .type(streamAppName)
      .getByTestId('new-steam-dialog')
      .within(() => {
<<<<<<< HEAD
        cy.getByText('ADD').click();
=======
        cy.findByText('Add').click();
>>>>>>> wip
      })
      .wait('@putPipeline');

    // TODO: these two topics can be added via API which should be faster than
    // adding from the UI
    cy.findByTestId('toolbar-topics')
      .click({ force: true })
<<<<<<< HEAD
      .getByText('Please select...')
      .click()
      .get(`li[data-value=${fromTopicName}]`)
      .click()
      .getByText('ADD')
=======
      .findByTestId('topic-select')
      .select(fromTopicName)
      .findByText('Add')
>>>>>>> wip
      .click()
      .wait('@putPipeline');

    cy.findByTestId('toolbar-topics')
      .click({ force: true })
<<<<<<< HEAD
      .getByText('Please select...')
      .click()
      .get(`li[data-value=${toTopicName}]`)
      .click()
      .getByText('ADD')
=======
      .findByTestId('topic-select')
      .select(toTopicName)
      .findByText('Add')
>>>>>>> wip
      .click()
      .wait('@putPipeline');

    cy.findByText(streamAppName)
      .click({ force: true })
      .findByTestId('from')
      .siblings()
      .then($els => {
        cy.wrap($els.first())
          .click()
          .get(`li[data-value=${fromTopicName}]`)
          .click();
      })
      .wait(2000) // UI has one sec throttle
      .wait('@putPipeline')
      .findByTestId('to')
      .siblings()
      .then($els => {
        cy.wrap($els.first())
          .click()
          .get(`li[data-value=${toTopicName}]`)
          .click();
      })
      .wait(2000) // UI has one sec throttle
      .wait('@putPipeline')
      .findByTestId('start-button')
      .click()
      .findByText('Stream app successfully started!')
      .findByText(streamAppName)
      .findByTestId('stop-button')
      .click();
  });
});
