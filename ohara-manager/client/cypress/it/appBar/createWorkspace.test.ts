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
import { generateNodeIfNeeded } from '../../utils';

describe('Create Workspace', () => {
  // generate node
  const node = generateNodeIfNeeded();

  before(() => cy.deleteAllServices());

  beforeEach(() => {
    cy.server();
    // our tests should begin from home page
    cy.visit('/');
  });

  context('When creating a workspace failed', () => {
    it('should be able to cancel ', () => {
      const workspaceName = generate.serviceName({ prefix: 'wk' });

      // mock the API to create a worker, return 500 error
      cy.route({
        method: 'POST',
        url: 'api/workers',
        status: 500,
        response: {
          code: 'DataCheckException',
          message: 'node does not exist',
          stack: 'mock stack',
        },
      });

      cy.createNodeIfNotExists(node);
      cy.createWorkspace({
        workspaceName,
        node,
        closeOnFailureOrFinish: false,
      });

      // when an error occurs, the CANCEL button should allow clicking
      cy.findByText('CANCEL').click();
      // when the cancellation is completed, the CLOSE button should allow clicking
      cy.findByText('CLOSE').click();

      cy.closeIntroDialog();

      // the workspace just canceled should not exist
      cy.reload();
      cy.visit(`/${workspaceName}`)
        .location()
        .should('not.eq', `/${workspaceName}`);
    });

    it('should have event logs', () => {
      const workspaceName = generate.serviceName({ prefix: 'wk' });

      // mock the API to start a worker, return 400 error
      cy.route({
        method: 'PUT',
        url: 'api/workers/*/start**',
        status: 400,
        response: {
          code: 'java.lang.IllegalArgumentException',
          message: `Does not have image:oharastream/connect-worker`,
          stack: `mock stack`,
        },
      });

      cy.createNodeIfNotExists(node);
      cy.createWorkspace({
        workspaceName,
        node,
      });

      cy.closeIntroDialog();
      cy.findByTitle('Event logs').click();
      cy.findByTestId('event-log-list').within(() => {
        cy.findAllByText(
          `Start workers "${workspaceName}" failed. --> Does not have image:oharastream/connect-worker`,
        ).should('exist');
        cy.findAllByText(`Failed to create workspace ${workspaceName}.`).should(
          'exist',
        );
      });
    });

    it('should display an indicator for Unstable workspace', () => {
      const workspaceName = generate.serviceName({ prefix: 'wk' });

      // mock the API to create a brokers, return 500 error
      cy.route({
        method: 'POST',
        url: 'api/brokers',
        status: 500,
        response: {
          code: 'mock code',
          message: 'mock message',
          stack: 'mock stack',
        },
      });

      cy.createNodeIfNotExists(node);
      cy.createWorkspace({
        workspaceName,
        node,
      });

      cy.closeIntroDialog();

      // should highlight the unstable workspaces
      cy.get('#app-bar')
        .find(`div.workspace-list > span[title="${workspaceName}"]`)
        .within(() => {
          cy.findByTitle('Unstable workspace').should('exist');
        });
    });

    it('should not be able to use unstable workspace', () => {
      const workspaceName = generate.serviceName({ prefix: 'wk' });

      // mock the API to create a zookeeper, return 500 error
      cy.route({
        method: 'POST',
        url: 'api/zookeepers',
        status: 500,
        response: {
          code: 'mock code',
          message: 'mock message',
          stack: 'mock stack',
        },
      });

      cy.createNodeIfNotExists(node);
      cy.createWorkspace({
        workspaceName,
        node,
      });

      cy.closeIntroDialog();

      // clicking the button of unstable workspace should show a snackbar
      cy.get('#app-bar')
        .find(`div.workspace-list > span[title="${workspaceName}"]`)
        .as('unstableWorkspaceBtn')
        .click();
      cy.findByTestId('snackbar')
        .contains(`This is an unstable workspace: ${workspaceName}`)
        .should('exist');

      // cannot use URL to access unstable workspace
      cy.visit(`/${workspaceName}`)
        .location()
        .should('not.eq', `/${workspaceName}`);

      // open the workspace list dialog
      cy.findByTestId('workspace-list-button').click();
      // buttons of unstable workspaces should be disabled
      cy.findByTestId('workspace-list-dialog')
        .should('exist')
        .contains('div.MuiCard-root', workspaceName)
        .contains('UNSTABLE WORKSPACE')
        .should('be.disabled');
    });
  });
  it('If "close after finish" is checked, it should be closed automatically', () => {
    const workspaceName = generate.serviceName({ prefix: 'wk' });

    // Click the quickstart dialog
    cy.visit('/');

    // Since we will redirect the url
    // need to wait a little time for url applying
    cy.wait(1000);

    cy.closeIntroDialog();

    cy.findByTitle('Create a new workspace').click();
    cy.findByText('QUICK CREATE').should('exist').click();

    // Step1: workspace name
    if (workspaceName) {
      // type the workspaceName by parameter
      cy.findByDisplayValue('workspace', { exact: false })
        .clear()
        .type(workspaceName);
    }
    cy.findAllByText('NEXT').filter(':visible').click();

    // Step2: select nodes
    // we wait a little time for the "click button" to be rendered
    cy.wait(1000);
    cy.contains('p:visible', 'Click here to select nodes').click();
    cy.addNode(node);

    // Step3: create workspace
    cy.wait(1000);
    cy.findAllByText('SUBMIT').filter(':visible').click();

    cy.findByTestId('create-workspace-progress-dialog').should('be.visible');
    cy.findByText('Close after finish').click();

    cy.findByTestId('create-workspace-progress-dialog').should(
      'not.be.visible',
    );
  });
});
