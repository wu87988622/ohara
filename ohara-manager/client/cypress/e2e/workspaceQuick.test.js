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

import { MODE } from '../../src/const';
import * as inspectApi from '../../src/api/inspectApi';
import { deleteAllServices } from '../utils';
import * as generate from '../../src/utils/generate';

let mode;
const nodeHost = Cypress.env('nodeHost');
const nodePort = Cypress.env('nodePort');
const nodeUser = Cypress.env('nodeUser');
const nodePass = Cypress.env('nodePass');
const prefix = Cypress.env('servicePrefix');

// It's uppercase in AppBar
const workspaceNameInAppBar = prefix.substring(0, 2).toUpperCase();

describe('Create workspaces', () => {
  beforeEach(async () => {
    await deleteAllServices();
    const res = await inspectApi.getConfiguratorInfo();
    mode = res.data.mode;
  });

  it('using quick mode with default to create workspace', () => {
    // Use different combination of commands to add node in different modes
    if (mode === MODE.docker) {
      Cypress.Commands.add('addNode', () => {
        cy.contains('button', 'Click here to select nodes').click();
        cy.contains('button', /^add node$/i).click();
        cy.get('input[name=hostname]').type(nodeHost);
        cy.get('input[name=port]').type(nodePort);
        cy.get('input[name=user]').type(nodeUser);
        cy.get('input[name=password]').type(nodePass);
        cy.findByText(/^add$/i)
          .click()
          .findByText(nodeHost)
          .click()
          .findByText(/^save$/i)
          .click();
        cy.findAllByText(/^next$/i)
          .filter(':visible')
          .click();
      });
    } else if (mode === MODE.k8s) {
      Cypress.Commands.add('addNode', () => {
        cy.contains('button', 'Click here to select nodes').click();
        cy.findByText(nodeHost)
          .click()
          .findByText(/^save$/i)
          .click();
        cy.findAllByText(/^next$/i)
          .filter(':visible')
          .click();
      });
    }

    // Click the quickstart dialog
    cy.visit('/');
    cy.findByText(/^quick start$/i)
      .should('exist')
      .click();

    // Step1: workspace name (using default)
    cy.findAllByText(/^next$/i)
      .filter(':visible')
      .click();

    // Step2: select nodes
    cy.addNode();

    // Step3: add worker plugins (using default)
    cy.findAllByText(/^next$/i)
      .filter(':visible')
      .click();

    // Step4: create workspace
    cy.findAllByText(/^finish$/i)
      .filter(':visible')
      .click();

    // the default workspace name is "quickstartXXX"
    // hence, the first two words should be "QU"
    cy.findByText(/^qu$/i).should('exist');

    // create another workspace
    const workspaceName = generate.serviceName({ prefix });
    cy.findByText(/^qu$/i)
      .parent('span')
      .siblings('')
      .should('have.length', 1)
      .click();
    cy.findByText(/^quick create$/i)
      .should('exist')
      .click();

    // Step1: workspace name (using generate name)
    cy.findByDisplayValue('quickworkspace2')
      .clear()
      .type(workspaceName)
      .findAllByText(/^next$/i)
      .filter(':visible')
      .click();

    // Step2: select nodes (we had add node already, continue directly)
    cy.findByText(nodeHost).should('exist');
    cy.findAllByText(/^next$/i)
      .filter(':visible')
      .click();

    // Step3: add worker plugins (using default)
    cy.findAllByText(/^next$/i)
      .filter(':visible')
      .click();

    // Step4: create workspace
    cy.findAllByText(/^finish$/i)
      .filter(':visible')
      .click();

    cy.findByText(workspaceNameInAppBar).should('exist');

    // we have two workspace now
    cy.findByTitle('Workspace list')
      .click()
      .findByText(/^showing 2 workspaces$/i)
      .should('exist');
  });
});
