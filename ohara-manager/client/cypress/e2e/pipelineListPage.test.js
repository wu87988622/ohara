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

describe('PipelineListPage', () => {
  before(() => {
    cy.removeWorkers();
    cy.addWorker();
  });

  beforeEach(() => {
    cy.server();
    cy.route('GET', 'api/pipelines').as('getPipelines');
    cy.route('PUT', 'api/pipelines/*').as('putPipeline');
    cy.route('POST', 'api/pipelines').as('postPipeline');
    cy.route('DELETE', 'api/pipelines/*').as('deletePipeline');
  });

  it('should display a newly created pipeline in the list', () => {
    const pipelineName = generate.serviceName({
      prefix: 'pipeline',
    });

    cy.visit(URLS.PIPELINES)
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
      .wait('@postPipeline')
      .wait('@getPipelines')
      .findByText(pipelineName)
      .should('have.length', 1);
  });

  it('edits a pipeline', () => {
    const pipelineName = generate.serviceName({ prefix: 'pipeline' });
    const pipelineParams = {
      name: pipelineName,
      group: `${Cypress.env('WORKER_NAME')}${pipelineName}`,
      tags: {
        workerClusterName: Cypress.env('WORKER_NAME'),
      },
    };

    cy.addPipeline(pipelineParams)
      .visit(URLS.PIPELINES)
      .wait('@getPipelines')
      .findByText(pipelineName)
      .then($el => {
        cy.wrap($el.parent()).within(() => {
          cy.findByTestId('edit-pipeline').click({ force: true });
        });
      })
      .location('pathname')
      .should('contains', `${URLS.PIPELINES}/edit`);
  });

  it('deletes a pipeline', () => {
    const pipelineName = generate.serviceName({ prefix: 'pipeline' });
    const pipelineParams = {
      name: pipelineName,
      group: `${Cypress.env('WORKER_NAME')}${pipelineName}`,
      tags: {
        workerClusterName: Cypress.env('WORKER_NAME'),
      },
    };

    cy.addPipeline(pipelineParams)
      .visit(URLS.PIPELINES)
      .wait('@getPipelines')
      .findByText(pipelineName)
      .then($el => {
        cy.wrap($el.parent()).within(() => {
          cy.findByTestId('delete-pipeline').click({ force: true });
        });
      })
      .findByText('DELETE')
      .click({ force: true })
      .wait('@deletePipeline')
      .findByText(`Successfully deleted the pipeline: ${pipelineName}`)
      .should('have.length', 1);
  });
});
