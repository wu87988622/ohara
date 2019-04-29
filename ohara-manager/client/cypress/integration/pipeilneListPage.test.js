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

describe('PipelineListPage', () => {
  before(() => {
    cy.deleteWorker();
    cy.createWorker();
  });

  it('should display an newly created pipeline in the list', () => {
    cy.server();
    cy.route('GET', 'api/pipelines').as('getPipelines');
    cy.route('PUT', 'api/pipelines/*').as('putPipeline');
    cy.route('POST', 'api/pipelines').as('postPipeline');

    const pipelineName = makeRandomStr();

    cy.visit(URLS.PIPELINE)
      .getByText('New pipeline')
      .click()
      .getByTestId('cluster-select')
      .select(Cypress.env('WORKER_NAME'))
      .getByText('Next')
      .click()
      .wait('@postPipeline')
      .getByText('Untitled pipeline')
      .click({ force: true })
      .getByTestId('title-input')
      .clear()
      .type(pipelineName)
      .blur()
      .wait('@putPipeline')
      .visit(URLS.PIPELINE)
      .wait('@getPipelines')
      .getByText(pipelineName)
      .should('have.length', 1);
  });

  it('edits a pipeline', () => {
    cy.server();
    cy.route('GET', 'api/pipelines').as('getPipelines');

    const pipelineName = makeRandomStr();
    const pipelineParams = {
      name: pipelineName,
      workerName: Cypress.env('WORKER_NAME'),
    };

    cy.createPipeline(pipelineParams)
      .visit(URLS.PIPELINE)
      .wait('@getPipelines')
      .getByTestId('edit-pipeline')
      .click()
      .location('pathname')
      .should('contains', `${URLS.PIPELINE}/edit`);
  });

  it('deletes a pipeline', () => {
    cy.server();
    cy.route('GET', 'api/pipelines').as('getPipelines');
    cy.route('DELETE', 'api/pipelines/*').as('deletePipeline');

    const pipelineName = makeRandomStr();
    const pipelineParams = {
      name: pipelineName,
      workerName: Cypress.env('WORKER_NAME'),
    };

    cy.createPipeline(pipelineParams)
      .visit(URLS.PIPELINE)
      .wait('@getPipelines')
      .getByText(pipelineName)
      .then($el => {
        cy.wrap($el.parent()).within(() => {
          cy.getByTestId('delete-pipeline').click();
        });
      })
      .getByText('Yes, Delete this pipeline')
      .click()
      .wait('@deletePipeline')
      .getByText(`Successfully deleted the pipeline: ${pipelineName}`)
      .should('have.length', 1);
  });
});
