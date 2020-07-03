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
import { KIND } from '../../../src/const';
import { NodeRequest } from '../../../src/api/apiInterface/nodeInterface';
import {
  SOURCES,
  SINKS,
} from '../../../src/api/apiInterface/connectorInterface';

const ACTIONS = {
  link: 'link',
  config: 'config',
  remove: 'remove',
};

const node: NodeRequest = {
  hostname: generate.serviceName(),
  port: generate.port(),
  user: generate.userName(),
  password: generate.password(),
};

describe('Paper Element connections', () => {
  before(() => {
    cy.deleteAllServices();
    cy.createWorkspace({ node });
    cy.uploadStreamJar();
  });

  beforeEach(() => {
    cy.deleteAllPipelines();
    cy.createPipeline();
  });

  context('Basic connection', () => {
    it('should verify connect logic between Paper elements', () => {
      // Prepare elements
      const perfSourceName = generate.serviceName({ prefix: 'source' });
      const ftpSourceName = generate.serviceName({ prefix: 'source' });
      const consoleSinkName = generate.serviceName({ prefix: 'sink' });
      const hdfsSinkName = generate.serviceName({ prefix: 'sink' });
      const pipelineOnlyTopicName1 = 'T1';
      const pipelineOnlyTopicName2 = 'T2';

      const elements = [
        {
          name: perfSourceName,
          kind: KIND.source,
          className: SOURCES.perf,
        },
        {
          name: ftpSourceName,
          kind: KIND.source,
          className: SOURCES.ftp,
        },
        {
          name: pipelineOnlyTopicName1,
          kind: KIND.topic,
          className: KIND.topic,
        },
        {
          name: consoleSinkName,
          kind: KIND.sink,
          className: SINKS.console,
        },
        {
          name: pipelineOnlyTopicName2,
          kind: KIND.topic,
          className: KIND.topic,
        },
        {
          name: hdfsSinkName,
          kind: KIND.sink,
          className: SINKS.hdfs,
        },
      ];

      // Add to Paper
      elements.forEach(({ name, kind, className }) => {
        cy.addElement(name, kind, className);
      });

      // ⛔️ Cannot link two source connectors together: perfSource -> ftpSource
      cy.getCell(perfSourceName).trigger('mouseover');
      cy.cellAction(perfSourceName, ACTIONS.link).click();
      cy.getCell(ftpSourceName).click();
      cy.findByText(`Target ${ftpSourceName} is a source!`)
        .should('exist')
        .siblings('div')
        .first()
        .click();

      // ⛔️ Cannot link two topics together: pipelineOnlyTopic1 -> pipelineOnlyTopic2
      cy.getCell(pipelineOnlyTopicName1).trigger('mouseover');
      cy.cellAction(pipelineOnlyTopicName1, ACTIONS.link).click();
      cy.getCell(pipelineOnlyTopicName2).click();
      cy.findByText(
        `Cannot connect a ${KIND.topic} to another ${KIND.topic}, they both have the same type`,
      )
        .should('exist')
        .siblings('div')
        .first()
        .click();

      // Create a connection between PerfSource -> pipelineOnlyTopic1 -> hdfsSink
      cy.getCell(perfSourceName).trigger('mouseover');
      cy.cellAction(perfSourceName, ACTIONS.link).click();
      cy.getCell(pipelineOnlyTopicName1).click();
      cy.getCell(pipelineOnlyTopicName1).trigger('mouseover');
      cy.cellAction(pipelineOnlyTopicName1, ACTIONS.link).click();
      cy.getCell(hdfsSinkName).click();

      // Should create two links
      cy.get('#paper .joint-link').should('have.length', 2);

      // ⛔️ Cannot link a source to a sink where the sink already has a connection: ftpSource -> hdfsSink
      cy.getCell(ftpSourceName).trigger('mouseover');
      cy.cellAction(ftpSourceName, ACTIONS.link).click();
      cy.getCell(hdfsSinkName).click();
      cy.findByText(
        `The target ${hdfsSinkName} is already connected to a source`,
      )
        .should('exist')
        .siblings('div')
        .first()
        .click();

      // ⛔️ Cannot link a topic to a sink where the sink already has a connection: pipelineOnlyTopic2 -> hdfsSink
      cy.getCell(pipelineOnlyTopicName2).trigger('mouseover');
      cy.cellAction(pipelineOnlyTopicName2, ACTIONS.link).click();
      cy.getCell(hdfsSinkName).click();
      cy.findByText(
        `The target ${hdfsSinkName} is already connected to a source`,
      )
        .should('exist')
        .siblings('div')
        .first()
        .click();
    });

    it('should able to delete a connected topic', () => {
      const perfSourceName = generate.serviceName({ prefix: 'source' });
      const pipelineOnlyTopicName = 'T1';
      const smbSinkName = generate.serviceName({ prefix: 'sink' });

      const elements = [
        {
          name: perfSourceName,
          kind: KIND.source,
          className: SOURCES.perf,
        },
        {
          name: pipelineOnlyTopicName,
          kind: KIND.topic,
          className: KIND.topic,
        },
        {
          name: smbSinkName,
          kind: KIND.sink,
          className: SINKS.smb,
        },
      ];

      elements.forEach(({ name, kind, className }) => {
        cy.addElement(name, kind, className);
      });

      // Create the connections
      cy.getCell(perfSourceName).trigger('mouseover');
      cy.cellAction(perfSourceName, ACTIONS.link).click();
      cy.getCell(pipelineOnlyTopicName).click();

      cy.getCell(pipelineOnlyTopicName).trigger('mouseover');
      cy.cellAction(pipelineOnlyTopicName, ACTIONS.link).click();
      cy.getCell(smbSinkName).click();

      cy.get('#paper .paper-element').should('have.length', 3);
      cy.get('#paper .joint-link').should('have.length', 2);

      // Start pipeline
      cy.startPipeline('pipeline1');
      cy.get('#paper .paper-element .running').should('have.length', 3);

      // we can delete a connected topic and remove the links
      cy.removeElement(pipelineOnlyTopicName);

      cy.findByText(pipelineOnlyTopicName).should('not.exist');
      cy.get('#paper .paper-element').should('have.length', 2);
      cy.get('#paper .joint-link').should('have.length', 0);
    });

    it('should create a full connection', () => {
      const perfSourceName = generate.serviceName({ prefix: 'source' });
      const ftpSourceName = generate.serviceName({ prefix: 'source' });
      const consoleSinkName = generate.serviceName({ prefix: 'sink' });
      const hdfsSinkName = generate.serviceName({ prefix: 'sink' });
      const pipelineOnlyTopicName1 = 'T1';
      const streamName = generate.serviceName({ prefix: 'stream' });
      const pipelineOnlyTopicName2 = 'T2';

      cy.addElement(perfSourceName, KIND.source, SOURCES.perf);
      cy.addElement(ftpSourceName, KIND.source, SOURCES.ftp);
      cy.addElement(consoleSinkName, KIND.sink, SINKS.console);
      cy.addElement(hdfsSinkName, KIND.sink, SINKS.hdfs);
      cy.addElement(pipelineOnlyTopicName1, KIND.topic);
      cy.addElement(streamName, KIND.stream);
      cy.addElement(pipelineOnlyTopicName2, KIND.topic);

      // 1. perf source -> topic1 -> stream -> topic2 -> hdfs sink
      cy.getCell(perfSourceName).trigger('mouseover');
      cy.cellAction(perfSourceName, ACTIONS.link).click();
      cy.getCell(pipelineOnlyTopicName1).click();

      cy.getCell(pipelineOnlyTopicName1).trigger('mouseover');
      cy.cellAction(pipelineOnlyTopicName1, ACTIONS.link).click();
      cy.getCell(streamName).click();

      cy.getCell(streamName).trigger('mouseover');
      cy.cellAction(streamName, ACTIONS.link).click();
      cy.getCell(pipelineOnlyTopicName2).click();

      cy.getCell(pipelineOnlyTopicName2).trigger('mouseover');
      cy.cellAction(pipelineOnlyTopicName2, ACTIONS.link).click();
      cy.getCell(hdfsSinkName).click();

      // Should create four links
      cy.get('#paper .joint-link').should('have.length', 4);
    });
  });

  context('Automatically topic creation', () => {
    it('should create a new topic between a source and sink connection', () => {
      const shabondiSourceName = generate.serviceName({ prefix: 'source' });
      const hdfsSinkName = generate.serviceName({ prefix: 'sink' });

      cy.addElement(shabondiSourceName, KIND.source, SOURCES.shabondi);
      cy.addElement(hdfsSinkName, KIND.sink, SINKS.hdfs);

      cy.getCell(shabondiSourceName).trigger('mouseover');
      cy.cellAction(shabondiSourceName, ACTIONS.link).click();
      cy.getCell(hdfsSinkName).click();

      // It should automatically generate a "T1" topic and create two links
      cy.get('#paper').findByText('T1').should('exist');
      cy.get('#paper .joint-link').should('have.length', 2);
    });

    it('should create a new topic between a source and stream connection', () => {
      const jdbcSourceName = generate.serviceName({ prefix: 'source' });
      const streamName = generate.serviceName({ prefix: 'stream' });

      cy.addElement(jdbcSourceName, KIND.source, SOURCES.jdbc);
      cy.addElement(streamName, KIND.stream, KIND.stream);

      cy.getCell(jdbcSourceName).trigger('mouseover');
      cy.cellAction(jdbcSourceName, ACTIONS.link).click();
      cy.getCell(streamName).click();

      // It should automatically generate a "T1" topic and create two links
      cy.get('#paper').findByText('T1').should('exist');
      cy.get('#paper .joint-link').should('have.length', 2);
    });

    it('should create a new topic between a stream and sink connection', () => {
      const streamName = generate.serviceName({ prefix: 'stream' });
      const shabondiSink = generate.serviceName({ prefix: 'source' });

      cy.addElement(streamName, KIND.stream, KIND.stream);
      cy.addElement(shabondiSink, KIND.sink, SINKS.shabondi);

      cy.getCell(streamName).trigger('mouseover');
      cy.cellAction(streamName, ACTIONS.link).click();
      cy.getCell(shabondiSink).click();

      // It should automatically generate a "T1" topic and create two links
      cy.get('#paper').findByText('T1').should('exist');
      cy.get('#paper .joint-link').should('have.length', 2);
    });
  });
});
