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

import { hashByGroupAndName } from '../../src/utils/sha';

interface FixtureResponse {
  name: string;
  fileList: FileList;
  file: File;
  group: string;
  tags?: object;
}

declare global {
  namespace Cypress {
    type FixtureRequest = {
      fixturePath: string;
      name: string;
      group: string;
      tags?: object;
    };

    interface Chainable {
      createJar: (file: FixtureRequest) => Promise<FixtureResponse>;
      dragAndDrop: (
        shiftX: number,
        shiftY: number,
      ) => Chainable<JQuery<HTMLElement>>;
      createWorkspace: ({
        workspaceName,
        withPlugin,
      }: {
        workspaceName?: string;
        withPlugin?: boolean;
      }) => Chainable<null>;
      addNode: () => Chainable<null>;
      addElement: (
        name: string,
        kind: string,
        className?: string,
      ) => Chainable<null>;
      getCell: (name: string) => Chainable<HTMLElement>;
      cellAction: (name: string, action: string) => Chainable<HTMLElement>;
    }
  }
}

const nodeHost = Cypress.env('nodeHost');
const nodePort = Cypress.env('nodePort');
const nodeUser = Cypress.env('nodeUser');
const nodePass = Cypress.env('nodePass');

// Utility commands
Cypress.Commands.add('createJar', (file: Cypress.FixtureRequest) => {
  const { fixturePath, name, group: jarGroup, tags: jarTags } = file;
  cy.fixture(`${fixturePath}/${name}`, 'base64')
    .then(Cypress.Blob.base64StringToBlob)
    .then(blob => {
      const blobObj = blob as Blob;
      const type = 'application/java-archive';
      const testFile = new File([blobObj], name, { type });
      const dataTransfer = new DataTransfer();
      dataTransfer.items.add(testFile);
      const fileList = dataTransfer.files;
      const params: FixtureResponse = {
        name,
        fileList,
        file: fileList[0],
        group: jarGroup,
      };
      if (jarTags) params.tags = jarTags;
      return params;
    });
});

Cypress.Commands.add(
  'dragAndDrop',
  { prevSubject: true },
  (subject: HTMLElement, shiftX: number, shiftY: number) => {
    cy.wrap(subject)
      // using the top-left position to trigger the event
      // since we calculate the moving event by rect.left and rect.top
      .trigger('mousedown', 'topLeft', { timeout: 1000, which: 1 });
    // we only get one "flying element" at one time
    // it's ok to find by testid
    cy.findByTestId('flying-element').then(element => {
      cy.wrap(element)
        .trigger('mousemove', 'topLeft', {
          timeout: 1000,
          pageX: shiftX,
          pageY: shiftY,
          force: true,
        })
        .trigger('mouseup', 'topLeft', { timeout: 1000, force: true });
    });
  },
);

Cypress.Commands.add(
  'createWorkspace',
  ({
    workspaceName,
    withPlugin = true,
  }: {
    workspaceName?: string;
    withPlugin?: boolean;
  } = {}) => {
    Cypress.Commands.add('addNode', () => {
      cy.get('body').then($body => {
        const isDockerMode =
          $body.find('button > span:contains("ADD NODE")').length > 0;

        if (isDockerMode) {
          cy.get('body').then($body => {
            // the node has not been added yet, added directly
            if ($body.find(`td:contains(${nodeHost})`).length === 0) {
              cy.contains('button', /^add node$/i).click();
              cy.get('input[name=hostname]').type(nodeHost);
              cy.get('input[name=port]').type(nodePort);
              cy.get('input[name=user]').type(nodeUser);
              cy.get('input[name=password]').type(nodePass);
              cy.findByText(/^add$/i).click();
            }
            cy.findByText(nodeHost)
              .siblings('td')
              .find('input[type="checkbox"]')
              .click()
              .findByText(/^save$/i)
              .click();
            cy.findAllByText(/^next$/i)
              .filter(':visible')
              .click();
          });
        } else {
          cy.findByText(nodeHost)
            .siblings('td')
            .find('input[type="checkbox"]')
            .click()
            .findByText(/^save$/i)
            .click();
          cy.findAllByText(/^next$/i)
            .filter(':visible')
            .click();
        }
        cy.end();
      });
    });

    // Click the quickstart dialog
    cy.visit('/');

    // Since we will redirect the url
    // need to wait a little time for url applying
    cy.wait(2000);

    cy.location().then(location => {
      if (location.pathname === '/') {
        // first time in homepage, close the helper quickMode dialog
        cy.findByTestId('close-intro-button').click();
      }
      cy.findByTitle('Create a new workspace').click();
      cy.findByText(/^quick create$/i)
        .should('exist')
        .click();
    });

    // Step1: workspace name
    if (workspaceName) {
      // type the workspaceName by parameter
      cy.findByDisplayValue('workspace', { exact: false })
        .clear()
        .type(workspaceName);
    }
    cy.findAllByText(/^next$/i)
      .filter(':visible')
      .click();

    // Step2: select nodes
    // we wait a little time for the "click button" to be rendered
    cy.wait(1000);
    cy.contains('p:visible', 'Click here to select nodes').click();
    cy.addNode();

    // Step3: add worker plugins
    // we add a stream.jar for testing purpose

    if (withPlugin) {
      const workspaceKey = {
        name: 'workspace1',
        group: 'workspace',
      };
      const fileGroup = hashByGroupAndName(
        workspaceKey.group,
        workspaceKey.name,
      );
      const stream = {
        fixturePath: 'stream',
        name: 'ohara-it-stream.jar',
        group: fileGroup,
        tags: { parentKey: workspaceKey },
      };
      cy.contains('button:visible', 'Add worker plugins')
        .find('input')
        .then(element => {
          cy.createJar(stream).then(params => {
            element[0].files = params.fileList;
            cy.wrap(element).trigger('change', { force: true });
          });
        });
      cy.wait(1000);
      cy.contains('button', 'Add worker plugins').click();
    }

    cy.findAllByText(/^next$/i)
      .filter(':visible')
      .click();

    // Step4: create workspace
    cy.wait(1000);
    cy.findAllByText(/^next$/i)
      .filter(':visible')
      .click();

    cy.findByTitle('Workspace list')
      .children()
      .should('not.be.disabled');

    cy.end();
  },
);
