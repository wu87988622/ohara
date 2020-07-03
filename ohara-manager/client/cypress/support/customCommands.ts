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
import { deleteAllServices } from '../utils';

interface FixtureResponse {
  name: string;
  fileList: FileList;
  file: File;
  group: string;
  tags?: object;
}

export enum SETTING_SECTIONS {
  topics = 'Topics',
  autofill = 'Autofill',
  zookeeper = 'Zookeeper',
  broker = 'Broker',
  worker = 'Worker',
  stream = 'Stream',
  nodes = 'Nodes',
  files = 'Files',
  dangerZone = 'Danger Zone',
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
      deleteAllServices: () => Chainable<null>;
      /**
       * Get the _&lt;td /&gt;_ elements by required parameters.
       * <p> This function has the following combination:
       *
       * <p> 1. `columnName`: filter all _&lt;td /&gt;_ elements of specific column.
       *
       * <p> 2. `columnName + columnValue`: filter the _&lt;td /&gt;_ element of specific column and value.
       *
       * <p> 3. `columnName + rowFilter`: filter the _&lt;td /&gt;_ element of specific column in specific rows.
       *
       * <p> 4. `columnName + columnValue + rowFilter`: filter the _&lt;td /&gt;_ element of specific column in specific rows.
       *
       * @param {string} columnName the filtered header of table cell
       * @param {string} columnValue the filtered value of table cell
       * @param {Function} rowFilter given a function to filter the result of elements
       */
      getTableCellByColumn: (
        $table: JQuery<HTMLTableElement>,
        columnName: string,
        columnValue?: string,
        rowFilter?: (row: JQuery<HTMLTableElement>) => boolean,
      ) => Chainable<JQuery<HTMLElement | HTMLElement[]>>;
      // Paper
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
      removeElement: (name: string) => Chainable<null>;
      getCell: (name: string) => Chainable<HTMLElement>;
      cellAction: (name: string, action: string) => Chainable<HTMLElement>;
      uploadStreamJar: () => Chainable<null>;
      // Pipeline
      createPipeline: (name?: string) => Chainable<null>;
      deletePipeline: (name: string) => Chainable<null>;
      deleteAllPipelines: (name?: string) => Chainable<null>;
      // Settings
      switchSettingSection: (
        section: SETTING_SECTIONS,
        listItem?: string,
      ) => Chainable<null>;
      createSharedTopic: (name?: string) => Chainable<null>;
    }
  }
}

// if the Cypress.env is undefined and no node paramter specify
// we generate random value for it
let nodeHost =
  Cypress.env('nodeHost') || generate.serviceName({ prefix: 'node' });
let nodePort = Cypress.env('nodePort') || generate.port().toString();
let nodeUser = Cypress.env('nodeUser') || generate.userName();
let nodePass = Cypress.env('nodePass') || generate.password();

const workspaceGroup = 'workspace';
const workerGroup = 'worker';

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
          cy.findByText('CREATE').click();
        }
        cy.findByText(nodeHost)
          .siblings('td')
          .find('input[type="checkbox"]')
          .click();
        cy.findByText('SAVE').click();
        cy.findAllByText('NEXT').filter(':visible').click();
      });
      cy.end();
    });

    // Click the quickstart dialog
    cy.visit('/');

    // Since we will redirect the url
    // need to wait a little time for url applying
    cy.wait(1000);

    cy.location().then((location) => {
      if (location.pathname === '/') {
        // first time in homepage, click quick create directly
        cy.findByText('QUICK CREATE').should('exist').click();
      } else {
        cy.findByTitle('Create a new workspace').click();
        cy.findByText('QUICK CREATE').should('exist').click();
      }
    });

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
    cy.addNode();

    // Step3: create workspace
    cy.wait(1000);
    cy.findAllByText('SUBMIT').filter(':visible').click();

    cy.findByTestId('create-workspace').should('be.visible');
    cy.findByTestId('stepper-close-button').should('be.visible').click();
    cy.findByTestId('create-workspace').should('not.be.visible');
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

Cypress.Commands.add('deleteAllServices', () => {
  cy.log(`Begin delete all services...`)
    .then(async () => await deleteAllServices())
    .then(() => cy.log(`Finish delete all services!`));
});

Cypress.Commands.add(
  'getTableCellByColumn',
  // this action should be a parent command
  { prevSubject: false },
  (
    $table: JQuery<HTMLTableElement>,
    columnName: string,
    columnValue?: string,
    rowFilter?: (row: JQuery<HTMLTableElement>) => boolean,
  ) => {
    cy.log(
      `Find column['${columnName}'] with value['${columnValue}'] from table`,
    );
    const header = $table.find('thead tr').find(`th:contains("${columnName}")`);
    const index = $table.find('thead tr th').index(header);

    let tableRows = null;
    if (rowFilter) {
      tableRows = $table
        .find('tbody tr')
        .filter((_, element) => rowFilter(Cypress.$(element)));
    } else {
      tableRows = $table.find('tbody tr');
    }

    const finalElement = columnValue
      ? tableRows.has(`td:contains("${columnValue}")`).length === 0
        ? null
        : tableRows.find(`td:contains("${columnValue}")`)
      : tableRows
          .map((_, element) => Cypress.$(element).find('td').eq(index))
          .get()
          .shift();
    return cy.wrap(finalElement);
  },
);

// Settings
Cypress.Commands.add(
  'switchSettingSection',
  (section: SETTING_SECTIONS, listItem?: string) => {
    cy.get('body').then(($body) => {
      // check whether we are in the homepage or not
      if (
        $body.find('div[data-testid="workspace-settings-dialog"]').length > 0
      ) {
        // force to visit the root path
        cy.visit('/');
      }
      cy.get('#navigator').within(() => {
        cy.get('button').should('have.length', 1).click();
      });
    });

    // enter the settings dialog
    cy.findAllByRole('menu').filter(':visible').find('li').click();

    if (listItem) {
      cy.contains('h2', section)
        .parent('section')
        .find('ul')
        .contains('li', listItem)
        .should('have.length', 1)
        .as('section');
    } else {
      cy.contains('h2', section)
        .parent('section')
        .find('ul')
        .should('have.length', 1)
        .as('section');
    }

    cy.get('@section')
      // We need to "offset" the element we just scrolled from the header of Settings
      // which has 64px height
      .scrollIntoView({ offset: { top: -64, left: 0 } })
      .should('be.visible')
      .click();
  },
);

Cypress.Commands.add(
  'createSharedTopic',
  (name = generate.serviceName({ prefix: 'topic' })) => {
    cy.switchSettingSection(SETTING_SECTIONS.topics);

    // add shared topics
    cy.findByTitle('Create Topic').should('be.enabled').click();

    cy.findAllByRole('dialog')
      .filter(':visible')
      .within(() => {
        cy.findAllByLabelText('Topic name', { exact: false })
          .filter(':visible')
          .type(name);
        cy.findAllByLabelText('Partitions', { exact: false })
          .filter(':visible')
          .type('1');
        cy.findAllByLabelText('Replication factor', { exact: false })
          .filter(':visible')
          .type('1');
        cy.contains('button', 'CREATE').click();
      });

    cy.get('.shared-topic:visible')
      .find('table')
      .within(($table) => {
        cy.getTableCellByColumn($table, 'Name', name).should('exist');
        cy.getTableCellByColumn($table, 'State')
          .invoke('html')
          .should('equal', 'RUNNING');
      });

    cy.findByTestId('workspace-settings-dialog-close-button').click({
      force: true,
    });
  },
);
