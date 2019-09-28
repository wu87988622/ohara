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

describe('Header', () => {
  beforeEach(() => {
    cy.visit(URLS.HOME);
  });

  it('visits all pages from main navigation', () => {
    cy.get('nav').within(() => {
      cy.findByText('Pipelines')
        .click()
        .location('pathname')
        .should('eq', URLS.PIPELINES);

      cy.findByText('Nodes')
        .click()
        .location('pathname')
        .should('eq', URLS.NODES);

      cy.findByText('Workspaces')
        .click()
        .location('pathname')
        .should('contains', URLS.WORKSPACES);
    });
  });

  it('shows ohara version info', () => {
    cy.request('GET', 'api/info')
      .then(({ body: { versionInfo: { date, revision, version }, mode } }) => {
        // we only need these four properties
        return { date, revision, version, mode };
      })
      .as('info');

    cy.findByTestId('version-btn')
      .click()
      .findByTestId('info-modal')
      .then($el => {
        cy.wrap($el)
          .should('be.visible')
          .findByText('Ohara version')
          .should('be.visible');

        cy.get('@info').then(info => {
          const { version, revision, date, mode } = info;

          cy.findByText(version)
            .findByText(mode)
            .findByText(revision)
            .findByText(date);

          // Ensure we only render these element in the screen
          cy.findByTestId('info-list')
            .find('li')
            .should('have.length', Object.keys(info).length);
        });
      });
  });
});
