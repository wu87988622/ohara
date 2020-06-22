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

// Note: Do not change the usage of absolute path
// unless you have a solution to resolve TypeScript + Coverage
import { KIND, CELL_TYPES } from '../../src/const';
import * as generate from '../../src/utils/generate';
import { SOURCES, SINKS } from '../../src/api/apiInterface/connectorInterface';
import { deleteAllServices } from '../utils';

const ACTIONS = {
  link: 'link',
  config: 'config',
  remove: 'remove',
};

describe('Paper Element connections', () => {
  before(async () => await deleteAllServices());

  it('tests of connector and topic links in pipeline', () => {
    cy.createWorkspace({});
    cy.createPipeline();

    // wait 20s for the connectors loaded into toolbox
    cy.wait(20000);

    // force to reload the page in order to get the correct data in toolbox
    cy.reload();

    // check the toolbox
    cy.findByText(/^toolbox$/i).should('exist');

    // conditional tests of element link
    const elements = {
      perfSourceName: generate.serviceName({ prefix: 'source' }),
      ftpSourceName: generate.serviceName({ prefix: 'source' }),
      consoleSinkName: generate.serviceName({ prefix: 'sink' }),
      hdfsSinkName: generate.serviceName({ prefix: 'sink' }),
      pipelineOnlyTopicName1: 'T1',
      pipelineOnlyTopicName2: 'T2',
    };

    cy.log('Add elements:');
    cy.log(`Element: perf source ${elements.perfSourceName}`);
    cy.log(`Element: ftp source ${elements.ftpSourceName}`);
    cy.log(`Element: pipeline-only topic 1 ${elements.pipelineOnlyTopicName1}`);
    cy.log(`Element: console sink ${elements.consoleSinkName}`);
    cy.log(`Element: pipeline-only topic 2 ${elements.pipelineOnlyTopicName2}`);
    cy.log(`Element: hdfs sink ${elements.hdfsSinkName}`);
    cy.addElement(elements.perfSourceName, KIND.source, SOURCES.perf);
    cy.addElement(elements.ftpSourceName, KIND.source, SOURCES.ftp);
    cy.addElement(elements.pipelineOnlyTopicName1, KIND.topic);
    cy.addElement(elements.consoleSinkName, KIND.sink, SINKS.console);
    cy.addElement(elements.pipelineOnlyTopicName2, KIND.topic);
    cy.addElement(elements.hdfsSinkName, KIND.sink, SINKS.hdfs);

    // let the backend API makes effect
    cy.wait(5000);

    // 1. perf source -> ftp source
    cy.log(`Cannot create a link from perf source to ftp source`);
    cy.getCell(elements.perfSourceName).trigger('mouseover');
    cy.cellAction(elements.perfSourceName, ACTIONS.link).click();
    cy.getCell(elements.ftpSourceName).click();
    cy.findByText(`Target ${elements.ftpSourceName} is a source!`)
      .should('exist')
      .siblings('div')
      .first()
      .click();

    // 2. pipeline-only topic 1 -> pipeline-only topic 2
    cy.log(`Cannot create a link between pipeline-only topic 1 and 2`);
    cy.getCell(elements.pipelineOnlyTopicName1).trigger('mouseover');
    cy.cellAction(elements.pipelineOnlyTopicName1, ACTIONS.link).click();
    cy.getCell(elements.pipelineOnlyTopicName2).click();
    cy.findByText(
      `Cannot connect a ${KIND.topic} to another ${KIND.topic}, they both have the same type`,
    )
      .should('exist')
      .siblings('div')
      .first()
      .click();

    // 3. pipeline-only topic 1 -> hdfs sink
    cy.log(
      `Create a link from a perf source to pipeline-only topic 1 to hdfs sink`,
    );
    cy.getCell(elements.perfSourceName).trigger('mouseover');
    cy.cellAction(elements.perfSourceName, ACTIONS.link).click();
    cy.getCell(elements.pipelineOnlyTopicName1).click();
    cy.getCell(elements.pipelineOnlyTopicName1).trigger('mouseover');
    cy.cellAction(elements.pipelineOnlyTopicName1, ACTIONS.link).click();
    cy.getCell(elements.hdfsSinkName).click();
    // will create two links
    cy.get(`g[data-type="${CELL_TYPES.LINK}"]`).should('have.length', 2);

    // 4. ftp source -> hdfs sink
    cy.log(`Cannot create a link from ftp source to hdfs sink`);
    cy.getCell(elements.ftpSourceName).trigger('mouseover');
    cy.cellAction(elements.ftpSourceName, ACTIONS.link).click();
    cy.getCell(elements.hdfsSinkName).click();
    cy.findByText(
      `The target ${elements.hdfsSinkName} is already connected to a source`,
    )
      .should('exist')
      .siblings('div')
      .first()
      .click();

    // 5. pipeline-only topic 2 -> hdfs sink
    cy.log(`Cannot create a link from pipeline-only topic to hdfs sink`);
    cy.getCell(elements.pipelineOnlyTopicName2).trigger('mouseover');
    cy.cellAction(elements.pipelineOnlyTopicName2, ACTIONS.link).click();
    cy.getCell(elements.hdfsSinkName).click();
    cy.findByText(
      `The target ${elements.hdfsSinkName} is already connected to a source`,
    )
      .should('exist')
      .siblings('div')
      .first()
      .click();

    // we can force delete a connected topic
    cy.log(`force delete pipeline-only topic`);
    cy.getCell(elements.pipelineOnlyTopicName1).trigger('mouseover');
    cy.cellAction(elements.pipelineOnlyTopicName1, ACTIONS.remove).click();
    cy.findByText(/^delete$/i).should('exist');
    cy.contains('span:visible', /cancel/i).click();
    cy.findAllByText(elements.pipelineOnlyTopicName1).should('exist');

    // delete all elements
    Object.values(elements).forEach((element) => {
      cy.getCell(element).trigger('mouseover');
      cy.cellAction(element, ACTIONS.remove).click();
      cy.findByText(/^delete$/i).click();
      cy.findAllByText(element).filter(':visible').should('not.exist');
      cy.wait(2000);
    });
  });

  it('tests of stream, connector and topic links in pipeline', () => {
    cy.visit('/');

    // check the toolbox
    cy.findByText(/^toolbox$/i).should('exist');

    // conditional tests of element link
    const elements = {
      perfSourceName: generate.serviceName({ prefix: 'source' }),
      ftpSourceName: generate.serviceName({ prefix: 'source' }),
      consoleSinkName: generate.serviceName({ prefix: 'sink' }),
      hdfsSinkName: generate.serviceName({ prefix: 'sink' }),
      topicName1: 'T1',
      streamName: generate.serviceName({ prefix: 'stream' }),
      topicName2: 'T2',
    };

    // Prepare a stream jar for this test case
    cy.uploadStreamJar();

    cy.log('Add elements:');
    cy.log(`Element: perf source ${elements.perfSourceName}`);
    cy.log(`Element: ftp source ${elements.ftpSourceName}`);
    cy.log(`Element: console sink ${elements.consoleSinkName}`);
    cy.log(`Element: hdfs sink ${elements.hdfsSinkName}`);
    cy.log(`Element: pipeline-only topic 1 ${elements.topicName1}`);
    cy.log(`Element: stream ${elements.streamName}`);
    cy.log(`Element: pipeline-only topic 2 ${elements.topicName2}`);
    cy.addElement(elements.perfSourceName, KIND.source, SOURCES.perf);
    cy.addElement(elements.ftpSourceName, KIND.source, SOURCES.ftp);
    cy.addElement(elements.consoleSinkName, KIND.sink, SINKS.console);
    cy.addElement(elements.hdfsSinkName, KIND.sink, SINKS.hdfs);
    cy.addElement(elements.topicName1, KIND.topic);
    cy.addElement(elements.streamName, KIND.stream);
    cy.addElement(elements.topicName2, KIND.topic);

    // let the backend API makes effect
    cy.wait(5000);

    // 1. perf source -> topic1 -> stream -> topic2 -> hdfs sink
    cy.log(
      `Create a link from perf source to topic 1 to stream to topic 2 to hdfs sink`,
    );
    cy.getCell(elements.perfSourceName).trigger('mouseover');
    cy.cellAction(elements.perfSourceName, ACTIONS.link).click();
    cy.getCell(elements.topicName1).click();

    cy.getCell(elements.topicName1).trigger('mouseover');
    cy.cellAction(elements.topicName1, ACTIONS.link).click();
    cy.getCell(elements.streamName).click();

    cy.getCell(elements.streamName).trigger('mouseover');
    cy.cellAction(elements.streamName, ACTIONS.link).click();
    cy.getCell(elements.topicName2).click();

    cy.getCell(elements.topicName2).trigger('mouseover');
    cy.cellAction(elements.topicName2, ACTIONS.link).click();
    cy.getCell(elements.hdfsSinkName).click();
    // will create four lines
    cy.get(`g[data-type="${CELL_TYPES.LINK}"]`).should('have.length', 4);

    // ftp source -> stream
    cy.log(`Cannot create a link from ftp source to stream`);
    cy.getCell(elements.ftpSourceName).trigger('mouseover');
    cy.cellAction(elements.ftpSourceName, ACTIONS.link).click();
    cy.getCell(elements.streamName).click();
    cy.findByText(
      `The target ${elements.streamName} is already connected to a source`,
    )
      .should('exist')
      .siblings('div')
      .first()
      .click();
  });
});

describe('Topic Operations of Pipeline', () => {
  before(async () => await deleteAllServices());
  it('connect two elements of pipeline should auto generate pipeline-only topic', () => {
    cy.createWorkspace({});
    cy.createPipeline();

    // check the toolbox
    cy.findByText(/^toolbox$/i).should('exist');

    // Ensure Toolbox items are ready
    cy.findByText('FtpSource').should('exist');

    const elements = {
      perfSourceName: generate.serviceName({ prefix: 'source' }),
      consoleSinkName: generate.serviceName({ prefix: 'sink' }),
      ftpSourceName: generate.serviceName({ prefix: 'source' }),
      hdfsSinkName: generate.serviceName({ prefix: 'sink' }),
      streamName1: generate.serviceName({ prefix: 'stream' }),
      streamName2: generate.serviceName({ prefix: 'stream' }),
      streamName3: generate.serviceName({ prefix: 'stream' }),
    };

    // Prepare a stream jar for this test case
    cy.uploadStreamJar();

    cy.log('Add elements:');
    cy.log(`Element: perf source ${elements.perfSourceName}`);
    cy.log(`Element: console sink ${elements.consoleSinkName}`);
    cy.log(`Element: ftp source ${elements.ftpSourceName}`);
    cy.log(`Element: hdfs sink ${elements.hdfsSinkName}`);
    cy.log(`Element: stream1 ${elements.streamName1}`);
    cy.log(`Element: stream2 ${elements.streamName2}`);
    cy.log(`Element: stream3 ${elements.streamName3}`);
    cy.addElement(elements.perfSourceName, KIND.source, SOURCES.perf);
    cy.addElement(elements.consoleSinkName, KIND.sink, SINKS.console);
    cy.addElement(elements.ftpSourceName, KIND.source, SOURCES.ftp);
    cy.addElement(elements.hdfsSinkName, KIND.sink, SINKS.hdfs);
    cy.addElement(elements.streamName1, KIND.stream);
    cy.addElement(elements.streamName2, KIND.stream);
    cy.addElement(elements.streamName3, KIND.stream);

    // let the backend API makes effect
    cy.wait(5000);

    cy.log(`Auto generate topic for perf source -> console sink`);
    cy.getCell(elements.perfSourceName).trigger('mouseover');
    cy.cellAction(elements.perfSourceName, ACTIONS.link).click();
    cy.getCell(elements.consoleSinkName).click();

    // generate "T1" pipeline-only topic
    cy.findAllByText('T1').should('exist');
    // will create two lines
    cy.get(`g[data-type="${CELL_TYPES.LINK}"]`).should('have.length', 2);

    // topic creation is a heavy request...we need to wait util ready
    cy.wait(5000);

    cy.log(`Auto generate topic for ftp source -> stream1`);
    cy.getCell(elements.ftpSourceName).trigger('mouseover');
    cy.cellAction(elements.ftpSourceName, ACTIONS.link).click();
    cy.getCell(elements.streamName1).click();

    // generate "T2" pipeline-only topic
    cy.findAllByText('T2').should('exist');
    // will create two lines (total four lines)
    cy.get(`g[data-type="${CELL_TYPES.LINK}"]`).should('have.length', 4);

    // topic creation is a heavy request...we need to wait util ready
    cy.wait(5000);

    cy.log(`Auto generate topic for stream2 -> stream3`);
    cy.getCell(elements.streamName2).trigger('mouseover');
    cy.cellAction(elements.streamName2, ACTIONS.link).click();
    cy.getCell(elements.streamName3).click();

    // generate "T3" pipeline-only topic
    cy.findAllByText('T3').should('exist');
    // will create two lines (total six lines)
    cy.get(`g[data-type="${CELL_TYPES.LINK}"]`).should('have.length', 6);

    // topic creation is a heavy request...we need to wait util ready
    cy.wait(5000);

    // click the settings dialog
    cy.findByText('workspace1').click();
    cy.contains('li', 'Settings').click();

    cy.findByText('Topics in this workspace').click({ force: true });
    cy.contains('td', 'T1').should('exist');
    cy.contains('td', 'T2').should('exist');
    cy.contains('td', 'T3').should('exist');
  });
});
