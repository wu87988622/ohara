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
import { makeServiceNames } from '../utils';

describe('PipelineNewPage', () => {
  const serviceNames = makeServiceNames();

  before(() => {
    cy.initServices(serviceNames);
  });

  after(() => {
    cy.clearServices(serviceNames);
  });

  beforeEach(() => {
    cy.visit(URLS.PIPELINE)
      .getByTestId('new-pipeline')
      .click()
      .getByText('Next')
      .click();
  });

  it('adds a new topic into pipeline graph', () => {
    cy.createTopic().as('createTopic');

    cy.getByTestId('toolbar-topics')
      .click()
      .get('@createTopic')
      .then(topic => {
        cy.getByTestId('topic-select').select(topic.name);
      })
      .getByText('Add')
      .click()
      .get('@createTopic')
      .then(topic => {
        cy.getByText(topic.name).should('be.exist');
      });
  });

  it('Creates a FTP source connector', () => {
    cy.location('pathname').should('contain', '/pipelines/new/');
    cy.getByTestId('toolbar-sources').click();

    cy.getByText('Add a new source connector').should('have.length', 1);
    cy.getByText('com.island.ohara.connector.ftp.FtpSource').click();

    cy.getByText('Add').click();

    cy.getByText('Untitled source')
      .should('have.length', '1')
      .get('.node-type')
      .should('contain', 'FtpSource');
  });
});
