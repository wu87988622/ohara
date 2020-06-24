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

import * as generate from '../../../src/utils/generate';

describe('App Bar', () => {
  const wkName = generate.serviceName();

  before(() => {
    cy.deleteAllServices();
    cy.createWorkspace({});
    // Sine we want to do assertion about the event log
    // make sure event log action is already finished
    cy.findByText('Successfully created workspace workspace1.', {
      timeout: 70000,
    }).should('not.be.visible');
  });

  beforeEach(() => {
    // our tests should begin from home page
    cy.visit('/');
  });

  context('Event Log', () => {
    it('should be able to delete event logs', () => {
      cy.createWorkspace({ workspaceName: wkName });
      // Sine we want to do assertion about the event log
      // make sure event log action is already finished
      cy.findByText('Successfully created workspace workspace1.', {
        timeout: 70000,
      }).should('not.be.visible');

      // we should have create workspace event log
      cy.findByTitle('Event logs').click();
      cy.findByTestId('event-log-list').within(() => {
        cy.findAllByText(`Successfully created workspace ${wkName}.`).should(
          'exist',
        );
      });

      // edit the event log setting
      cy.findByTitle('Event logs settings').click();
      // could not change the maximum number if checked unlimited
      cy.get('input[type="checkbox"]:visible').check();
      cy.findByPlaceholderText('The maximum amount of logs.').should(
        'be.disabled',
      );
      cy.findAllByText(/^save$/i)
        .filter(':visible')
        .click();
      cy.findByText('There are 2 currently').should('exist');

      // change maximum number of event logs
      cy.findByTitle('Event logs settings').click();
      cy.get('input[type="checkbox"]:visible').uncheck();
      // change maximum event log to 1
      cy.findByPlaceholderText('The maximum amount of logs.').clear().type('1');

      cy.findAllByText(/^save$/i)
        .filter(':visible')
        .click();
      // the event log should be still visible
      cy.findByTestId('event-log-list').within(() => {
        cy.findAllByText(`Successfully created workspace ${wkName}.`).should(
          'exist',
        );
      });

      // click remove button if there are some logs
      cy.findByTitle('Clear event logs')
        .find('button')
        .within(($el) => {
          const isDisabled = $el.is(':disabled');
          if (!isDisabled) {
            cy.wrap($el).click();
          }
        });

      // The event log message should be cleared by now
      cy.findByText('No log').should('exist');
    });
  });
});
