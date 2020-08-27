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

import { some } from 'lodash';
import {
  NodeRequest,
  NodeResponse,
} from '../../src/api/apiInterface/nodeInterface';
import { State } from '../../src/api/apiInterface/topicInterface';
import * as generate from '../../src/utils/generate';
import { deleteAllServices, generateNodeIfNeeded } from '../utils';
import { KIND } from '../../src/const';

interface FixtureResponse {
  name: string;
  fileList: FileList;
  file: File;
  group: string;
  tags?: object;
}

export interface ElementParameters {
  name: string;
  kind: KIND.stream | KIND.topic | KIND.source | KIND.sink | KIND.shabondi;
  className?: string;
}

export enum SETTING_SECTION {
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

export enum CELL_ACTION {
  link = 'link',
  config = 'config',
  remove = 'remove',
  start = 'start',
  stop = 'stop',
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
      // Utils
      createJar: (file: FixtureRequest) => Promise<FixtureResponse>;
      createNode: (node?: NodeRequest) => Chainable<NodeRequest>;
      createNodeIfNotExists: (node: NodeRequest) => Chainable<NodeResponse>;
      createWorkspace: ({
        workspaceName,
        node,
        closeOnFailureOrFinish,
      }: {
        workspaceName?: string;
        node?: NodeRequest;
        closeOnFailureOrFinish?: boolean;
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
      addNode: (node?: NodeRequest) => Chainable<null>;
      addElement: (element: ElementParameters) => Chainable<null>;
      addElements: (elements: ElementParameters[]) => Chainable<null>;
      removeElement: (name: string) => Chainable<null>;

      /**
       * Get a Paper element by name
       * @param {string} name Element name
       * @param {boolean} isTopic If element is a topic
       * @example cy.getElement('mySource').should('have.text', 'running');
       * @example cy.getElement('myTopic').should('have.class', 'running');
       */

      getElementStatus: (
        name: string,
        isTopic?: boolean,
      ) => Chainable<JQuery<HTMLElement>>;
      getCell: (name: string) => Chainable<HTMLElement>;
      cellAction: (name: string, action: CELL_ACTION) => Chainable<HTMLElement>;

      /**
       * Create a connection between elements
       * @param {string[]} elements Element list, the connection will be created following the list order
       * @param {boolean} waitForApiCall If the command should wait for pipeline update API call to finish or not
       * @example cy.createConnection(['ftpSource', 'topic1', 'consoleSink']); // create a connection of ftpSource -> topic1 -> consoleSink
       */

      createConnections: (
        elements: string[],
        waitForApiCall?: boolean,
      ) => Chainable<null>;
      uploadStreamJar: () => Chainable<null>;
      // Pipeline
      createPipeline: (name?: string) => Chainable<null>;
      startPipeline: (name: string) => Chainable<null>;
      stopPipeline: (name: string) => Chainable<null>;
      deletePipeline: (name: string) => Chainable<null>;
      stopAndDeleteAllPipelines: () => Chainable<null>;
      // Settings
      switchSettingSection: (
        section: SETTING_SECTION,
        listItem?: string,
      ) => Chainable<null>;
      createSharedTopic: (name?: string) => Chainable<null>;
      closeIntroDialog: () => Chainable<null>;
    }
  }
}

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
  'createNode',
  (node: NodeRequest = generateNodeIfNeeded()) => {
    // if the intro dialog appears, we should close it
    cy.get('body').then(($body) => {
      if ($body.find('[data-testid="intro-dialog"]').length > 0) {
        cy.findByTestId('close-intro-button').filter(':visible').click();
      }
    });

    cy.findByTestId('nodes-dialog-open-button').click();
    cy.findByTestId('nodes-dialog').should('exist');

    cy.findByRole('table').then(($table) => {
      if ($table.find(`td:contains(${node.hostname})`).length === 0) {
        cy.findByTitle('Create Node').click();
        cy.get('input[name=hostname]').type(node.hostname);
        cy.get('input[name=port]').type(`${node.port}`);
        cy.get('input[name=user]').type(node.user);
        cy.get('input[name=password]').type(node.password);
        cy.findByText('CREATE').click();
      }
    });

    cy.findByTestId('nodes-dialog-close-button').click();
    cy.findByTestId('nodes-dialog').should('not.exist');

    return cy.wrap(node);
  },
);

Cypress.Commands.add('createNodeIfNotExists', (nodeToCreate: NodeRequest) => {
  cy.request('api/nodes')
    .then((res) => res.body)
    .then((nodes) => {
      const isExist = some(nodes, {
        hostname: nodeToCreate.hostname,
      });
      if (isExist) {
        return cy.wrap(nodeToCreate);
      } else {
        // create if the node does not exist
        return cy
          .request('POST', 'api/nodes', nodeToCreate)
          .then((createdNode) => {
            return cy.wrap(createdNode);
          });
      }
    });
});

Cypress.Commands.add('addNode', (node = generateNodeIfNeeded) => {
  cy.get('body').then(($body) => {
    // the node has not been added yet, added directly
    if ($body.find(`td:contains(${node.hostname})`).length === 0) {
      cy.findByTitle('Create Node').click();
      cy.get('input[name=hostname]').type(node.hostname);
      cy.get('input[name=port]').type(String(node.port));
      cy.get('input[name=user]').type(node.user);
      cy.get('input[name=password]').type(node.password);
      cy.findByText('CREATE').click();
    }
    cy.findByText(node.hostname)
      .siblings('td')
      .find('input[type="checkbox"]')
      .click();
    cy.findByText('SAVE').click();
    cy.findAllByText('NEXT').filter(':visible').click();
  });
  cy.end();
});

Cypress.Commands.add(
  'createWorkspace',
  ({
    workspaceName,
    node = generateNodeIfNeeded(),
    closeOnFailureOrFinish = true,
  }: {
    workspaceName?: string;
    node?: NodeRequest;
    closeOnFailureOrFinish?: boolean;
  } = {}) => {
    // Click the quickstart dialog
    cy.visit('/');

    // Wait until page is loaded
    cy.wait(1000);
    cy.closeIntroDialog();

    cy.findByTitle('Create a new workspace').click();
    cy.findByText('QUICK CREATE').should('exist').click();

    // Step1: workspace name
    if (workspaceName) {
      cy.findByPlaceholderText('workspace1').clear().type(workspaceName);
    }

    cy.findAllByText('NEXT').filter(':visible').click();

    // Step2: select nodes
    cy.contains('p:visible', 'Click here to select nodes').click();
    cy.addNode(node);

    // Step3: set volume
    cy.findAllByText('NEXT').eq(1).filter(':visible').click();

    // Step4: create workspace
    cy.findAllByText('SUBMIT').filter(':visible').click();

    cy.findByTestId('create-workspace-progress-dialog').should('be.visible');
    cy.findByTestId('stepper-close-button').should('be.visible');

    if (closeOnFailureOrFinish) {
      // if the RETRY button is enabled, the task is stopped and has not been completed
      cy.get('body').then(($body) => {
        const $retryButton = $body.find(
          'button[data-testid="stepper-retry-button"]',
        );
        if ($retryButton.filter(':visible').length > 0) {
          // when we refresh the browser, the native alert should prompt
          // TODO: assert the alert should be appears [Tracked by https://github.com/oharastream/ohara/issues/5381]

          // when we click the CLOSE button, the ABORT confirm dialog should prompt
          cy.findByTestId('stepper-close-button').click();
          cy.findByTestId('abort-task-confirm-dialog').should('be.visible');
          cy.findByTestId('confirm-button-ABORT').should('be.visible').click();
        } else {
          cy.findByTestId('stepper-close-button').click();
        }
      });
      cy.findByTestId('create-workspace-progress-dialog').should(
        'not.be.visible',
      );
    }

    cy.end();
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
  (section: SETTING_SECTION, listItem?: string) => {
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
    cy.switchSettingSection(SETTING_SECTION.topics);

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
          .should('equal', State.RUNNING);
      });

    cy.findByTestId('workspace-settings-dialog-close-button').click({
      force: true,
    });
  },
);

Cypress.Commands.add('closeIntroDialog', () => {
  // if the intro dialog appears, we should close it
  cy.get('body').then(($body) => {
    if ($body.find('[data-testid="intro-dialog"]').length > 0) {
      cy.findByTestId('close-intro-button').filter(':visible').click();
    }
  });
  return cy.end();
});
