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

describe('PipelinePage', () => {
  beforeEach(() => {
    cy.visit(URLS.PIPELINE);
    cy.getByTestId('new-pipeline').click();
  });

  it('creates a pipeline and displays in the pipeline list page', () => {
    // This is needed so cypress can get the correct DOM element to act on
    cy.wait(300);

    const pipelineName = 'Test pipeline';

    cy.getByText('Untitled pipeline')
      .click({ force: true })
      .getByTestId('title-input')
      .clear()
      .type(pipelineName)
      .blur();

    cy.visit(URLS.HOME);
    cy.getByText(pipelineName).should('have.length', 1);
  });

  it('Creates a FTP source connector', () => {
    cy.location('pathname').should('contain', '/pipelines/new/');
    cy.getByTestId('toolbar-sources').click();

    cy.getByText('Add a new source connector').should('have.length', 1);
    cy.getByText('com.island.ohara.connector.ftp.FtpSource').click();

    cy.getByText('Add').click();

    cy.getByText('Untitled Source')
      .should('have.length', '1')
      .get('.node-type')
      .should('contain', 'FtpSource');
  });
});
