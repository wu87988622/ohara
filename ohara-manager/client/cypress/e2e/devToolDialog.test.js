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

import { deleteAllServices } from '../utils';

// It's uppercase in AppBar
const workspaceNameInAppBar = Cypress.env('servicePrefix')
  .substring(0, 2)
  .toUpperCase();

describe('DevToolDialog - Topics tab', () => {
  beforeEach(async () => await deleteAllServices());

  it('we should have empty topic list initially', () => {
    // Close the quickstart dialog
    cy.visit('/');
    cy.findByTestId('close-intro-button').click();

    // App bar is visible
    cy.findByTitle('Create a new workspace').should('be.visible');

    cy.findByTitle('Developer Tools')
      .click()
      .findByText(/^topics$/i)
      .should('exist');

    // check the topic Select component
    cy.findByTitle('Select topic')
      .should('have.length', 1)
      .click()
      .get('ul')
      .should('be.empty');
  });

  it('with an exist workspace, topic list of devTool should work normally', () => {
    cy.createServices({
      withWorkspace: true,
      withTopic: true,
    }).then(res => {
      cy.produceTopicData(res.workspaceName, res.topic)
        .visit('/', {
          onBeforeLoad(win) {
            // to surveillance the window.open() event
            // we stub it and do nothing
            cy.stub(win, 'open');
          },
        })
        .findByText(workspaceNameInAppBar)
        .should('exist');

      // Check the topic tab exist
      cy.findByTitle('Developer Tools').click();
      // Check the status bar of no data
      cy.findByText(/^no topic data$/i).should('exist');

      cy.findByTitle('Select topic')
        .click()
        .findByText(res.topic.name)
        .click();

      // Check the topic data detail view
      cy.findByTestId('view-topic-table')
        .should('exist')
        .get('tbody tr')
        .should('have.length.greaterThan', 0)
        .findByTestId('detail-view-icon-0')
        .click();

      // Check the status bar
      cy.findByText(/^10 rows per query$/i).should('exist');

      // the detail view should exist
      cy.findByTestId('topic-detail-view')
        .should('exist')
        .find('button')
        .click();

      // refresh button
      cy.findByTitle('Fetch the data again')
        .click()
        .get('table')
        .should('exist');

      // query button
      cy.findByTitle('Query with different parameters')
        .click()
        .findByText(/^query$/i)
        .click()
        .get('table')
        .should('exist');
      cy.findByText(/^rows per query$/i)
        .parent()
        .find('input[type=number]')
        .first()
        .type('{esc}');

      // open new window button
      cy.findByTitle('Open in a new window').click();
      cy.window()
        .its('open')
        .should('be.called');

      // verify the switching tab operations work normally
      cy.findByText(/^logs$/i).click();
      cy.findByTestId('log-type-select').click();
      cy.findByText(/^worker$/i).click();
      cy.findByTestId('view-log-list').should('exist');
      // switch back again should be ok
      cy.findAllByText(/^topics$/i)
        .parents('button')
        .first()
        .click();
      cy.findByTestId('view-topic-table').should('exist');

      // close button
      cy.findByTitle('Close this panel').click();
    });
  });
});

describe('DevToolDialog - Logs tab', () => {
  beforeEach(async () => await deleteAllServices());

  it('with an exist workspace, configurator log of devTool should work normally', () => {
    cy.createServices({
      withWorkspace: true,
    }).then(() => {
      cy.visit('/', {
        onBeforeLoad(win) {
          // to surveillance the window.open() event
          // we stub it and do nothing
          cy.stub(win, 'open');
        },
      })
        .findByText(workspaceNameInAppBar)
        .should('exist');

      // Check the log tab exist
      cy.findByTitle('Developer Tools')
        .click()
        .findByText(/^logs$/i)
        .click();

      // Check the status bar of no data
      cy.findByText(/^no log data$/i).should('exist');

      // Check the logType dropdown list
      cy.findByTestId('log-type-select').click();

      // Check the log data of configurator
      cy.findByText(/^configurator$/i).click();
      cy.findByTestId('view-log-list').should('exist');

      // Check the status bar
      cy.findByText(/^latest 10 minutes$/i).should('exist');

      // refresh button
      cy.get('span[title="Fetch the data again"] > button:enabled')
        .click()
        .findByTestId('view-log-list')
        .should('exist');

      // query button
      cy.get(
        'span[title="Query with different parameters"] > button:enabled',
      ).click();
      cy.findByTestId('log-query-popover')
        .find('button')
        .click()
        .findByTestId('view-log-list')
        .should('exist');
      cy.findByText(/^minutes per query$/i)
        .parent()
        .find('input[type=number]')
        .first()
        .type('{esc}');

      // open new window button
      cy.findByTitle('Open in a new window').click();
      cy.window()
        .its('open')
        .should('be.called');

      // close button
      cy.findByTitle('Close this panel').click();
    });
  });
});
