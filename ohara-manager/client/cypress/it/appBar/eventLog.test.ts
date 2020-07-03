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
import { hashByGroupAndName } from '../../../src/utils/sha';

describe('App Bar', () => {
  // fake workspace
  const wkName = generate.serviceName();

  before(() => {
    cy.deleteAllServices();
    cy.createWorkspace({});

    // Wait until event log is properly logged
    cy.findByTestId('snackbar')
      .as('snackbar')
      .findByText(/Successfully created workspace/, { timeout: 7000 })
      .should('not.exist');
  });

  beforeEach(() => {
    // our tests should begin from home page
    cy.visit('/');
  });

  context('Event Log', () => {
    it('should be able to delete event logs', () => {
      cy.createWorkspace({ workspaceName: wkName });
      const successFulMessage = `Successfully created workspace ${wkName}.`;

      // Wait until event log is properly logged
      cy.findByTestId('snackbar')
        .findByText(successFulMessage, { timeout: 7000 })
        .should('not.exist');

      // we should have create workspace event log
      cy.findByTitle('Event logs').click();
      cy.findByTestId('event-log-list').within(() => {
        cy.findAllByText(successFulMessage).should('exist');
      });

      // edit the event log setting
      cy.findByTitle('Event logs settings').click();
      // could not change the maximum number if checked unlimited
      cy.get('input[type="checkbox"]:visible').check();
      cy.findByPlaceholderText('The maximum amount of logs.').should(
        'be.disabled',
      );
      cy.findAllByText('SAVE').filter(':visible').click();
      cy.findByText('There are 2 currently').should('exist');

      // change maximum number of event logs
      cy.findByTitle('Event logs settings').click();
      cy.get('input[type="checkbox"]:visible').uncheck();
      // change maximum event log to 1
      cy.findByPlaceholderText('The maximum amount of logs.').clear().type('1');

      cy.findAllByText('SAVE').filter(':visible').click();
      // the event log should be still visible
      cy.findByTestId('event-log-list').within(() => {
        cy.findAllByText(successFulMessage).should('exist');
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

    it('should show the notifications when creating a pipeline failed', () => {
      cy.visit('/workspace1');

      // create a pipeline by native api
      cy.request('POST', 'api/pipelines', {
        name: 'p1',
        // group is the hash value of "workspace1 + workspace"
        group: hashByGroupAndName('workspace', 'workspace1'),
      });

      cy.createPipeline('p1');
      cy.findByTitle('Event logs').within(() => {
        //This is the notification; should have one error
        cy.contains('span', 1).should('exist');
      });

      cy.createPipeline('p1');
      cy.findByTitle('Event logs').within(() => {
        //This is the notification; should have two errors
        cy.contains('span', 2).should('exist');
      });

      // open event log will clear the notifications
      cy.findByTitle('Event logs').click();
      cy.findByTitle('Event logs').within(() => {
        cy.contains('span', 2).should('not.exist');
      });

      // the notification should be gone after opened event log
      cy.createPipeline('p1');
      cy.findByTitle('Event logs').within(() => {
        cy.get('span.MuiBadge-badge').should('not.be.visible');
      });

      // close the event log by clicking close button
      cy.findByTitle('Close event logs').click();
      cy.createPipeline('p1');
      cy.findByTitle('Event logs').within(() => {
        //We will have one notification now
        cy.contains('span', 1).should('exist');
      });

      // open event log will clear the notifications
      cy.findByTitle('Event logs').click();
      cy.findByTitle('Event logs').within(() => {
        cy.get('span.MuiBadge-badge').should('not.be.visible');
      });

      // open dev tool button will close event log too
      cy.findByTitle('Developer Tools').click();
      cy.createPipeline('p1');
      cy.findByTitle('Event logs').within(() => {
        //We will have one notification now
        cy.contains('span', 1).should('exist');
      });
    });
  });
});
