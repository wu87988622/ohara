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
import { NodeRequest } from '../../../src/api/apiInterface/nodeInterface';
import { KIND } from '../../../src/const';
import {
  SOURCES,
  SINKS,
} from '../../../src/api/apiInterface/connectorInterface';
import { isShabondi } from '../../../src/components/Pipeline/PipelineUtils';

const node: NodeRequest = {
  hostname: generate.serviceName(),
  port: generate.port(),
  user: generate.userName(),
  password: generate.password(),
};

/* eslint-disable @typescript-eslint/no-unused-expressions */
describe('Paper', () => {
  before(() => {
    cy.deleteAllServices();
    cy.createWorkspace({ node });
    cy.uploadStreamJar();
  });

  beforeEach(() => {
    cy.deleteAllPipelines();
    cy.createPipeline();
  });

  context('Canvas', () => {
    it('should render necessary tools in Paper', () => {
      cy.get('#toolbar').should('exist');
      cy.get('#toolbox').should('exist');

      cy.get('.joint-paper-grid')
        .should('exist')
        .then(($grid) => {
          // Should render grid image
          expect($grid.get(0).style.backgroundImage).to.exist;
        });
    });
  });

  // TODO: Add a test to verify element position could be saved too
  context('Save', () => {
    it('should able to add element and then save', () => {
      // No connector exists yet
      cy.get('#paper .connector').should('have.length', 0);

      // Add a SMB source
      cy.addElement(
        generate.serviceName({ prefix: 'source' }),
        KIND.source,
        SOURCES.smb,
      );

      // Should have one connector in the Paper
      cy.get('#paper .connector').should('have.length', 1);

      // Wait until the changes are saved.
      cy.wait(1000);

      cy.reload();

      // See if SMB is indeed saved and can be loaded again
      cy.get('#paper .connector').should('have.length', 1);
    });

    it('should able to remove element and then save', () => {
      // No connector exists yet
      cy.get('#paper .connector').should('have.length', 0);
      const sourceName = generate.serviceName({ prefix: 'source' });

      // Add a FTP source
      cy.addElement(sourceName, KIND.source, SOURCES.ftp);

      // Should have one connector in the Paper
      cy.get('#paper .connector').should('have.length', 1);

      // Wait until the changes are saved
      cy.wait(1000);

      // Remove the source
      cy.getCell(sourceName).trigger('mouseover');
      cy.cellAction(sourceName, 'remove').click();
      cy.findByTestId('delete-dialog').within(() =>
        cy.findByText(/^delete$/i).click(),
      );

      // Wait until the changes are saved
      cy.wait(1000);

      // The connector should be removed from Paper by now
      cy.get('#paper .connector').should('have.length', 0);
    });

    it('should able to add a connection and then save', () => {
      // There are many possible connection, here we are adding a simple one
      // Perf -> topic
      createSourceAndTopic();

      // We should have a link now
      cy.get('#paper .joint-link').should('have.length', 1);

      // Wait until the changes are saved
      cy.wait(1000);

      cy.reload();

      // The link can be reloaded
      cy.get('#paper .joint-link').should('have.length', 1);
    });

    it('should able to remove a connection and then save', () => {
      createSourceAndTopic();

      // We should have a link now
      cy.get('#paper .joint-link').should('have.length', 1);

      cy.get('#paper .joint-link').then(($link) => {
        // Remove the link
        cy.wrap($link).trigger('mouseover');
        cy.get('[joint-selector="delete-paper-link-button"]').click();
      });

      // Wait until the changes are saved
      cy.wait(1000);

      cy.reload();

      cy.get('#paper .joint-link').should('have.length', 0);
    });
  });

  context('Paper and devTools interaction', () => {
    it('should automatically switch devTool tab base on the selected element', () => {
      const sourceName = generate.serviceName({ prefix: 'source' });
      const sinkName = generate.serviceName({ prefix: 'sink' });
      const streamName = generate.serviceName({ prefix: 'stream' });
      const topicName1 = 'T1';
      const topicName2 = 'T2';

      // Creating elements
      const elements = [
        {
          name: sourceName,
          kind: KIND.source,
          className: SOURCES.perf,
        },
        { name: topicName1, kind: KIND.topic, className: KIND.topic },
        {
          name: streamName,
          kind: KIND.stream,
          className: KIND.stream,
        },
        { name: topicName2, kind: KIND.topic, className: KIND.topic },
        {
          name: sinkName,
          kind: KIND.sink,
          className: SINKS.shabondi,
        },
      ];

      elements.forEach((element) => {
        const { name, kind, className } = element;
        cy.addElement(name, kind, className);
      });

      // Ensure everything is added
      cy.get('#paper .paper-element').should('have.length', elements.length);

      // Create connection
      // source -> topic1
      cy.getCell(sourceName).trigger('mouseover');
      cy.cellAction(sourceName, 'link').click();
      cy.getCell(topicName1).click();

      // topic1 -> stream
      cy.getCell(topicName1).trigger('mouseover');
      cy.cellAction(topicName1, 'link').click();
      cy.getCell(streamName).click();

      // Update property dialog form and close it
      fillNodeName(streamName);

      // stream -> topic2
      cy.getCell(streamName).trigger('mouseover');
      cy.cellAction(streamName, 'link').click();
      cy.getCell(topicName2).click();

      // topic2 -> sink
      cy.getCell(topicName2).trigger('mouseover');
      cy.cellAction(topicName2, 'link').click();
      cy.getCell(sinkName).click();

      // Update property dialog form and close it
      fillNodeName(sinkName);

      cy.findByTestId('pipeline-controls-button').click();
      cy.findByTestId('pipeline-controls-dropdown').within(() => {
        cy.findByText('Start all components').click();
      });

      // Open devTool panel
      cy.findByTitle(/developer tools/i).click();

      // Make sure we're in the topic tab
      cy.get('#dev-tool')
        .findByText('TOPICS')
        .parent()
        .should('have.class', 'Mui-selected');

      elements.forEach(({ name, kind, className }) => {
        cy.getCell(name).click();

        cy.get('#dev-tool').within(() => {
          cy.findByText('LOGS').parent().should('have.class', 'Mui-selected');
          const logType = getLogType(kind, className);

          cy.findByText(logType).should('exist');
          cy.findByText(node.hostname).should('exist');
          cy.findByText('fake log').should('exist');

          if (isShabondi(className) || kind === KIND.stream) {
            cy.findByText(name).should('exist');
          }
        });
      });
    });
  });
});

function fillNodeName(elementName: string) {
  cy.getCell(elementName).trigger('mouseover');
  cy.cellAction(elementName, 'config').click();

  cy.findByLabelText('Node name list').click();
  cy.findByText(node.hostname)
    .should('exist')
    .find('input[type="checkbox"]')
    .check()
    .should('be.checked');

  cy.findByText('SAVE CHANGES').click();
}

function getLogType(kind: string, type: string) {
  if (isShabondi(type)) return KIND.shabondi;

  if (kind === KIND.source || kind === KIND.sink) return KIND.worker;
  if (kind === KIND.topic) return KIND.broker;
  if (kind === KIND.stream) return KIND.stream;

  throw new Error(`Unknown kind of ${kind} and ${type}`);
}

function createSourceAndTopic() {
  // Create a Perf source and than a pipeline only topic
  const sourceName = generate.serviceName({ prefix: 'source' });
  const topicName = 'T1';
  cy.addElement(sourceName, KIND.source, SOURCES.perf);
  cy.addElement(topicName, KIND.topic);

  // Then, link Perf source and Topic together
  cy.getCell(sourceName).trigger('mouseover');
  cy.cellAction(sourceName, 'link').click();
  cy.getCell(topicName).click();

  return { sourceName, topicName };
}
