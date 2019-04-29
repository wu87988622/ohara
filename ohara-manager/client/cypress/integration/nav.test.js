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

describe.skip('Header', () => {
  beforeEach(() => {
    cy.visit(URLS.HOME);
  });

  it('visits all pages from main navigation', () => {
    cy.get('nav').within(() => {
      cy.getByText('Pipelines')
        .click()
        .location('pathname')
        .should('eq', URLS.PIPELINE);

      cy.getByText('Nodes')
        .click()
        .location('pathname')
        .should('eq', URLS.NODES);

      cy.getByText('Services')
        .click()
        .location('pathname')
        .should('contains', URLS.SERVICES);
    });
  });

  it('shows ohara version info', () => {
    cy.request('GET', 'api/info')
      .then(({ body: { versionInfo } }) => versionInfo)
      .as('info');

    cy.getByTestId('version-btn')
      .click()
      .getByTestId('info-modal')
      .then($el => {
        cy.wrap($el)
          .should('be.visible')
          .getByText('Ohara version')
          .should('be.visible');

        cy.get('@info').then(({ version, revision, date }) => {
          cy.getByText(version)
            .should('exist')
            .getByText(revision)
            .should('exist')
            .getByText(date)
            .should('exist');
        });
      })
      .getByTestId('close-btn')
      .click()
      .queryByTestId('info-modal', { timeout: 500 }) // we don't want to throw here, so use queryByTestId instead of getByTestId
      .should('not.be.visible');
  });
});
