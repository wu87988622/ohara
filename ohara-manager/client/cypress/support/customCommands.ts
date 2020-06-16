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

/* eslint-disable no-unused-vars */
/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable no-console */

// Note: Do not change the usage of absolute path
// unless you have a solution to resolve TypeScript + Coverage
import '@testing-library/cypress/add-commands';

import * as connectorApi from '../../src/api/connectorApi';
import {
  NodeRequest,
  NodeResponse,
} from '../../src/api/apiInterface/nodeInterface';
import { ClusterResponse } from '../../src/api/apiInterface/clusterInterface';
import { TopicResponse } from '../../src/api/apiInterface/topicInterface';
import { SOURCES } from '../../src/api/apiInterface/connectorInterface';
import { hashByGroupAndName } from '../../src/utils/sha';
import * as generate from '../../src/utils/generate';
import { sleep } from '../../src/utils/common';

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
    type ServiceData = {
      workspaceName?: string;
      node?: NodeResponse;
      zookeeper?: ClusterResponse;
      broker?: ClusterResponse;
      worker?: ClusterResponse;
      topic?: TopicResponse;
    };

    interface Chainable {
      // Utils
      createJar: (file: FixtureRequest) => Promise<FixtureResponse>;
      createWorkspace: ({
        workspaceName,
        node,
      }: {
        workspaceName?: string;
        node?: NodeRequest;
      }) => Chainable<null>;
      produceTopicData: (
        workspaceName?: string,
        topicName?: string,
      ) => Chainable<void>;
      // Drag & Drop
      dragAndDrop: (
        shiftX: number,
        shiftY: number,
      ) => Chainable<JQuery<HTMLElement>>;
      addNode: () => Chainable<null>;
      addElement: (
        name: string,
        kind: string,
        className?: string,
      ) => Chainable<null>;
      getCell: (name: string) => Chainable<HTMLElement>;
      cellAction: (name: string, action: string) => Chainable<HTMLElement>;
      uploadStreamJar: () => Chainable<null>;
      createPipeline: (name?: string) => Chainable<null>;
      deletePipeline: (name: string) => Chainable<null>;
      deleteAllPipelines: (name?: string) => Chainable<null>;
    }
  }
}

const workspaceGroup = 'workspace';
const workerGroup = 'worker';

let nodeHost = Cypress.env('nodeHost');
let nodePort = Cypress.env('nodePort');
let nodeUser = Cypress.env('nodeUser');
let nodePass = Cypress.env('nodePass');

// Utility commands
Cypress.Commands.add('createJar', (file: Cypress.FixtureRequest) => {
  const { fixturePath, name, group: jarGroup, tags: jarTags } = file;
  cy.fixture(`${fixturePath}/${name}`, 'base64')
    .then(Cypress.Blob.base64StringToBlob)
    .then((blob) => {
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
  'createWorkspace',
  ({
    workspaceName,
    node,
  }: {
    workspaceName?: string;
    node?: NodeRequest;
  } = {}) => {
    Cypress.Commands.add('addNode', () => {
      // use passed node data if defined
      if (node) {
        nodeHost = node.hostname;
        nodePort = node.port;
        nodeUser = node.user;
        nodePass = node.password;
      }

      cy.get('body').then(($body) => {
        // the node has not been added yet, added directly
        if ($body.find(`td:contains(${nodeHost})`).length === 0) {
          cy.findByTitle('Create Node').click();
          cy.get('input[name=hostname]').type(nodeHost);
          cy.get('input[name=port]').type(nodePort);
          cy.get('input[name=user]').type(nodeUser);
          cy.get('input[name=password]').type(nodePass);
          cy.findByText(/^create$/i).click();
        }
        cy.findByText(nodeHost)
          .siblings('td')
          .find('input[type="checkbox"]')
          .click();
        cy.findByText(/^save$/i).click();
        cy.findAllByText(/^next$/i)
          .filter(':visible')
          .click();
      });
      cy.end();
    });

    // Click the quickstart dialog
    cy.visit('/');

    // Since we will redirect the url
    // need to wait a little time for url applying
    cy.wait(2000);

    cy.location().then((location) => {
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

    // Step3: create workspace
    cy.wait(1000);
    cy.findAllByText(/^submit$/i)
      .filter(':visible')
      .click();

    cy.findByTitle('Workspace list').children().should('not.be.disabled');

    cy.end();
  },
);

// Note: this custom command is a "heavy" command, may take almost 40 seconds to accomplish
// make sure you have set enough timeout of defaultCommandTimeout in cypress.e2e.json
Cypress.Commands.add(
  'produceTopicData',
  async (workspaceName?: string, topicName?: String) => {
    if (!workspaceName || !topicName) {
      console.error('the workspaceName and topicName are required fields');
      return;
    }
    const connector = {
      name: generate.serviceName({ prefix: 'perf' }),
      // it is ok to use random group here; we just need a temp connector to produce data
      group: generate.serviceName({ prefix: 'group' }),
      connector__class: SOURCES.perf,
      topicKeys: [
        {
          name: topicName,
          group: hashByGroupAndName(workspaceGroup, workspaceName),
        },
      ],
      workerClusterKey: {
        name: workspaceName,
        group: workerGroup,
      },
    };

    await connectorApi.create(connector);
    await connectorApi.start(connector);

    // sleep 10 seconds for perf connector to write some data to topic
    await sleep(10000);

    // remove this temp connector
    await connectorApi.forceStop(connector);
    await connectorApi.remove(connector);
  },
);
