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
import { makeRandomStr, makeServiceNames } from '../utils';

describe.skip('PipelineListPage', () => {
  const serviceNames = makeServiceNames();
  before(() => cy.initServices(serviceNames));
  after(() => cy.clearServices(serviceNames));

  it('should display an newly created pipeline in the list', () => {
    const pipelineName = makeRandomStr();

    cy.visit(URLS.PIPELINE)
      .getByText('New pipeline')
      .click()
      .getByText('Next')
      .click()
      .wait(300) // need this so cypress can find the right dom element and act on it later
      .getByText('Untitled pipeline')
      .click({ force: true })
      .getByTestId('title-input')
      .clear()
      .type(pipelineName)
      .blur()
      .visit(URLS.PIPELINE)
      .getByText(pipelineName)
      .should('have.length', 1)
      .deletePipeline(pipelineName);
  });

  it('edits a pipeline', () => {
    const pipelineName = makeRandomStr();
    const pipelineParams = {
      name: pipelineName,
      workerName: serviceNames.workerName,
    };

    cy.createPipeline(pipelineParams)
      .visit(URLS.PIPELINE)
      .getByTestId('edit-pipeline')
      .click()
      .location('pathname')
      .should('contains', `${URLS.PIPELINE}/edit`)
      .deletePipeline(pipelineName);
  });

  it('deletes a pipeline', () => {
    const pipelineName = makeRandomStr();
    const pipelineParams = {
      name: pipelineName,
      workerName: serviceNames.workerName,
    };

    cy.createPipeline(pipelineParams)
      .visit(URLS.PIPELINE)
      .getByText(pipelineName)
      .getByTestId('delete-pipeline')
      .click()
      .getByText('Yes, Delete this pipeline')
      .click()
      .getByText(`Successfully deleted the pipeline: ${pipelineName}`)
      .should('have.length', 1);
  });
});
