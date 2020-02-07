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

import * as connectorApi from '../../src/api/connectorApi';
import { deleteAllServices } from '../utils';

describe('Pipeline Page', () => {
  beforeEach(async () => await deleteAllServices());

  it('create a empty pipeline should work normally', () => {
    cy.createWorkspace();

    // Add new pipeline
    cy.findByText(/^pipelines$/i)
      .siblings('svg')
      .first()
      .click()
      .findByText(/^add a new pipeline$/i)
      .should('exist');

    cy.findByTestId('new-pipeline-dialog')
      .find('input')
      .type('pipeline1');

    cy.findByText(/^add$/i).click();
  });

  it('check the toolbox works normally', () => {
    cy.createWorkspace();

    // Add new pipeline
    cy.findByText(/^pipelines$/i)
      .siblings('svg')
      .first()
      .click()
      .findByText(/^add a new pipeline$/i)
      .should('exist');

    cy.findByTestId('new-pipeline-dialog')
      .find('input')
      .type('pipeline1');

    cy.findByText(/^add$/i).click();

    // check the toolbox
    cy.findByText(/^toolbox$/i).should('exist');
    cy.findByText(/^source$/i)
      .should('exist')
      .click();
    Object.values(connectorApi.connectorSources).forEach(clz => {
      const name = clz.slice(clz.lastIndexOf('.') + 1);
      cy.findByText(name).should('exist');
    });

    cy.findByText(/^topic$/i)
      .should('exist')
      .click();

    cy.findByText(/^stream$/i)
      .should('exist')
      .click();

    cy.findByText(/^sink$/i)
      .should('exist')
      .click();
    Object.values(connectorApi.connectorSinks).forEach(clz => {
      const name = clz.slice(clz.lastIndexOf('.') + 1);
      cy.findByText(name).should('exist');
    });

    // check the toolbox quick icon
    cy.findByText(/^insert$/i)
      .should('exist')
      .siblings('div')
      .first()
      .within(() => {
        cy.get('button').each(el => cy.wrap(el).click());
      });
    //after all clicks, the sink connector list should be visible
    cy.contains('span:visible', 'PerfSource').should('not.exist');
    cy.contains('span:visible', 'ConsoleSink').should('exist');

    // filter components in toolBox
    cy.get('input[placeholder="Search topic & connector..."]').type('ftp');
    cy.contains('span:visible', 'FtpSource').should('exist');
    cy.contains('span:visible', 'FtpSink').should('exist');

    cy.get('input[placeholder="Search topic & connector..."]')
      .clear()
      .type('console');
    cy.contains('span:visible', 'FtpSource').should('not.exist');
    cy.contains('span:visible', 'ConsoleSink').should('exist');

    cy.get('input[placeholder="Search topic & connector..."]')
      .clear()
      .type('fake');
    cy.contains('span:visible', 'FtpSource').should('not.exist');
    cy.contains('span:visible', 'FtpSink').should('not.exist');
  });
});
