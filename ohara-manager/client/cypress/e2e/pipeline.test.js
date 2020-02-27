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

import { KIND } from '../../src/const';
import { capitalize } from 'lodash';
import { connectorSources, connectorSinks } from '../../src/api/connectorApi';
import * as generate from '../../src/utils/generate';
import { deleteAllServices } from '../utils';

const ACTIONS = {
  link: 'link',
  config: 'config',
  remove: 'remove',
};
const sources = Object.values(connectorSources).sort();
const sinks = Object.values(connectorSinks).sort();
let topics = [];

Cypress.Commands.add('addElement', (name, kind, className) => {
  cy.log(`add element: ${name} of ${kind} with className ${className}`);
  // toolbox: 272 width + navigator: 240 width + appBar: 64 width, we need to avoid covering it
  const initialX = 600;
  // the controllers tab has approximate 72 height, we need to avoid covering it
  const initialY = 100;
  const shiftWidth = 350;
  const shiftHeight = 110;

  cy.get('body').then($body => {
    let size = topics.length;
    cy.log(
      'calculate the size of elements(source, sink, stream, topic) in pipeline',
    );
    if ($body.find('div.connector').length > 0)
      size = size + $body.find('div.connector').length;

    cy.findByText(capitalize(kind))
      .should('exist')
      .click();

    // re-render the cell position to maximize the available space
    // the view of cells will be a [n, 2] matrix
    const x = size % 2 === 0 ? initialX : initialX + shiftWidth;
    const y = initialY + ~~(size / 2) * shiftHeight;
    cy.log(`element position: ${x}, ${y}`);

    // wait a little time for the toolbox list rendered
    cy.wait(2000);

    if (kind === KIND.source || kind === KIND.sink) {
      const elementIndex =
        kind === KIND.source
          ? sources.indexOf(className)
          : sinks.indexOf(className);

      cy.findByTestId('toolbox-draggable')
        .find('g[data-type="html.Element"]:visible')
        // the element index to be added
        .eq(elementIndex)
        .dragAndDrop(x, y);

      // type the name and add
      cy.findByLabelText(`${capitalize(kind)} name`, { exact: false }).type(
        name,
      );
      cy.findAllByText(/^add$/i)
        .filter(':visible')
        .click();
    } else if (kind === KIND.topic) {
      topics.push(name);
      cy.log(`Available topics in this pipeline: ${topics.join(',')}`);
      if (!name.startsWith('T')) {
        // create a shared topic
        cy.findByText('Add topics')
          .siblings('button')
          .first()
          .click();

        cy.findAllByLabelText('topic name', { exact: false })
          .filter(':visible')
          .type(name);
        cy.findAllByLabelText('partitions', { exact: false })
          .filter(':visible')
          .type(1);
        cy.findAllByLabelText('replication factor', { exact: false })
          .filter(':visible')
          .type(1);
        cy.findAllByText(/^add$/i)
          .filter(':visible')
          .click();

        cy.findByText(name).should('exist');

        // wait a little time for the topic show in toolbox
        cy.wait(3000);

        cy.findByTestId('toolbox-draggable')
          .find('g[data-type="html.Element"]:visible')
          // the element index to be added
          // the pipeline-only element is always first, we shift one element
          .eq(topics.sort().indexOf(name) + 1)
          .dragAndDrop(x, y);
      } else {
        // create a pipeline-only topic
        cy.findByTestId('toolbox-draggable')
          .find('g[data-type="html.Element"]:visible')
          // the only "draggable" cell is pipeline-only topic
          .first()
          .dragAndDrop(x, y);
      }
    } else if (kind === KIND.stream) {
      cy.findByTestId('toolbox-draggable')
        .find('g[data-type="html.Element"]:visible')
        // we only got 1 class for the uploaded stream jar
        // it's ok to assert the first element is the "stream class"
        .eq(0)
        .dragAndDrop(x, y);

      // type the name and add
      cy.findByLabelText(`${capitalize(kind)} name`, { exact: false }).type(
        name,
      );
      cy.findAllByText(/^add$/i)
        .filter(':visible')
        .click();
    }

    // wait a little time for the cell added
    cy.wait(3000);

    // close this panel
    cy.findByText(capitalize(kind)).click();
  });
});

Cypress.Commands.add('getCell', name => {
  // open the cell menu
  cy.findAllByText(name)
    .filter(':visible')
    .should('exist')
    .parents(
      name.startsWith('topic') || name.startsWith('T')
        ? 'div.topic'
        : 'div.connector',
    )
    .first()
    .then(el => {
      const testId = el[0].getAttribute('data-testid');
      return cy.get(`g[model-id="${testId}"]`);
    });
});

Cypress.Commands.add('cellAction', (name, action) => {
  // open the cell menu
  cy.findAllByText(name)
    .filter(':visible')
    .should('exist')
    .parents(
      name.startsWith('topic') || name.startsWith('T')
        ? 'div.topic'
        : 'div.connector',
    )
    .first()
    .within(() => {
      cy.get(`button.${action}:visible`);
    });
});

describe('ToolBox of Pipeline', () => {
  before(async () => await deleteAllServices());

  it('create an empty pipeline should work normally', () => {
    cy.createWorkspace();

    // Add new pipeline
    cy.findByText(/^pipelines$/i)
      .siblings('svg')
      .first()
      .click()
      .findByText(/^add a new pipeline$/i)
      .should('exist');

    cy.findByTestId('new-pipeline-dialog')
      .find('input')
      .type('pipeline1');

    cy.findByText(/^add$/i).click();
  });

  it('check the toolbox works normally', () => {
    cy.visit('/');
    cy.findAllByText(/^wo$/i).should('exist');

    // Add new pipeline
    cy.findByText(/^pipelines$/i)
      .siblings('svg')
      .first()
      .click()
      .findByText(/^add a new pipeline$/i)
      .should('exist');

    cy.findByTestId('new-pipeline-dialog')
      .find('input')
      .type('pipeline1');

    cy.findByText(/^add$/i).click();

    // force to reload the page in order to get the connectors
    cy.reload();

    // check the toolbox
    cy.findByText(/^toolbox$/i).should('exist');

    cy.findByText(/^source$/i)
      .should('exist')
      .click();
    Object.values(sources).forEach(clz => {
      const name = clz.slice(clz.lastIndexOf('.') + 1);
      cy.findByText(name).should('exist');
    });

    cy.findByText(/^topic$/i)
      .should('exist')
      .click();

    cy.findByText(/^stream$/i)
      .should('exist')
      .click();

    cy.findByText(/^sink$/i)
      .should('exist')
      .click();
    Object.values(sinks).forEach(clz => {
      const name = clz.slice(clz.lastIndexOf('.') + 1);
      cy.findByText(name).should('exist');
    });

    // check the toolbox quick icon
    cy.findByText(/^insert$/i)
      .should('exist')
      .siblings('div')
      .first()
      .within(() => {
        cy.get('button').each(el => cy.wrap(el).click());
      });
    //after all clicks, the sink connector list should be visible
    cy.contains('span:visible', 'PerfSource').should('not.exist');
    cy.contains('span:visible', 'ConsoleSink').should('exist');

    // filter components in toolBox
    cy.findAllByPlaceholderText('Search topic & connector...')
      .filter(':visible')
      .type('ftp');
    cy.contains('span:visible', 'FtpSource').should('exist');
    cy.contains('span:visible', 'FtpSink').should('exist');

    cy.findAllByPlaceholderText('Search topic & connector...')
      .filter(':visible')
      .clear()
      .type('console');
    cy.contains('span:visible', 'FtpSource').should('not.exist');
    cy.contains('span:visible', 'ConsoleSink').should('exist');

    cy.findAllByPlaceholderText('Search topic & connector...')
      .filter(':visible')
      .clear()
      .type('fake');
    cy.contains('span:visible', 'FtpSource').should('not.exist');
    cy.contains('span:visible', 'FtpSink').should('not.exist');
  });
});

describe('Element Links of Pipeline', () => {
  before(async () => await deleteAllServices());
  it('tests of connector and topic links in pipeline', () => {
    cy.createWorkspace();

    // Add new pipeline
    cy.findByText(/^pipelines$/i)
      .siblings('svg')
      .first()
      .click()
      .findByText(/^add a new pipeline$/i)
      .should('exist');

    cy.findByTestId('new-pipeline-dialog')
      .find('input')
      .type('pipeline1');

    cy.findByText(/^add$/i).click();

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
      topicName: generate.serviceName({ prefix: 'topic' }),
      privateTopicName: 'T1',
    };

    cy.log('add elements:');
    cy.log(`element: perf source ${elements.perfSourceName}`);
    cy.log(`element: ftp source ${elements.ftpSourceName}`);
    cy.log(`element: shared topic ${elements.topicName}`);
    cy.log(`element: console sink ${elements.consoleSinkName}`);
    cy.log(`element: pipeline-only topic ${elements.privateTopicName}`);
    cy.log(`element: hdfs sink ${elements.hdfsSinkName}`);
    cy.addElement(elements.perfSourceName, KIND.source, connectorSources.perf);
    cy.addElement(elements.ftpSourceName, KIND.source, connectorSources.ftp);
    cy.addElement(elements.topicName, KIND.topic, null);
    cy.addElement(elements.consoleSinkName, KIND.sink, connectorSinks.console);
    cy.addElement(elements.privateTopicName, KIND.topic, null);
    cy.addElement(elements.hdfsSinkName, KIND.sink, connectorSinks.hdfs);

    // let the backend API makes effect
    cy.wait(5000);

    // to get the actual data-testid
    // we need to refresh the paper again
    cy.reload();

    // 1. cannot create link to a source
    cy.log(`cannot create a link from perf source to ftp source`);
    cy.getCell(elements.perfSourceName).trigger('mouseover');
    cy.cellAction(elements.perfSourceName, ACTIONS.link).click();
    cy.getCell(elements.ftpSourceName).click();
    cy.findByText(`Target ${elements.ftpSourceName} is a source!`)
      .should('exist')
      .siblings('div')
      .first()
      .click();

    // 2. cannot create link from topic to topic
    cy.log(`cannot create a link from shared topic to private topic`);
    cy.getCell(elements.topicName).trigger('mouseover');
    cy.cellAction(elements.topicName, ACTIONS.link).click();
    cy.getCell(elements.privateTopicName).click();
    cy.findByText(
      `Cannot connect a ${KIND.topic} to another ${KIND.topic}, they both have the same type`,
    )
      .should('exist')
      .siblings('div')
      .first()
      .click();

    // 3. perf source -> topic -> hdfs sink
    cy.log(`create a link from perf source to shared topic to hdfs sink`);
    cy.getCell(elements.perfSourceName).trigger('mouseover');
    cy.cellAction(elements.perfSourceName, ACTIONS.link).click();
    cy.getCell(elements.privateTopicName).click();
    cy.getCell(elements.privateTopicName).trigger('mouseover');
    cy.cellAction(elements.privateTopicName, ACTIONS.link).click();
    cy.getCell(elements.hdfsSinkName).click();
    // will create two lines
    cy.get('g[data-type="standard.Link"]').should('have.length', 2);

    // 4. we don't allow multiple links for same cell
    // perf source -> console sink
    cy.log(`cannot create a link from perf source to console sink`);
    cy.getCell(elements.perfSourceName).trigger('mouseover');
    cy.cellAction(elements.perfSourceName, ACTIONS.link).click();
    cy.getCell(elements.consoleSinkName).click();
    cy.findByText(
      `The source ${elements.perfSourceName} is already connected to a target`,
    )
      .should('exist')
      .siblings('div')
      .first()
      .click();

    // ftp source -> hdfs sink
    cy.log(`cannot create a link from ftp source to hdfs sink`);
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

    // perf source -> topic
    cy.log(`cannot create a link from perf source to shared topic`);
    cy.getCell(elements.perfSourceName).trigger('mouseover');
    cy.cellAction(elements.perfSourceName, ACTIONS.link).click();
    cy.getCell(elements.topicName).click();
    cy.findByText(
      `The source ${elements.perfSourceName} is already connected to a target`,
    )
      .should('exist')
      .siblings('div')
      .first()
      .click();

    // topic -> hdfs sink
    cy.log(`cannot create a link from shared topic to hdfs sink`);
    cy.getCell(elements.topicName).trigger('mouseover');
    cy.cellAction(elements.topicName, ACTIONS.link).click();
    cy.getCell(elements.hdfsSinkName).click();
    cy.findByText(
      `The target ${elements.hdfsSinkName} is already connected to a source`,
    )
      .should('exist')
      .siblings('div')
      .first()
      .click();

    // we can force delete an used topic
    cy.log(`force delete pipeline-only topic`);
    cy.getCell(elements.privateTopicName).trigger('mouseover');
    cy.cellAction(elements.privateTopicName, ACTIONS.remove).click();
    cy.findByText(/^delete$/i).should('exist');
    cy.contains('span:visible', /cancel/i).click();
    cy.findAllByText(elements.privateTopicName).should('exist');

    // delete all elements
    Object.values(elements).forEach(element => {
      cy.getCell(element).trigger('mouseover');
      cy.cellAction(element, ACTIONS.remove).click();
      cy.findByText(/^delete$/i).click();
      cy.findAllByText(element)
        .filter(':visible')
        .should('not.exist');
      cy.wait(2000);
    });

    // clear the global topics
    topics = [];
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
      topicName1: generate.serviceName({ prefix: 'topic' }),
      streamName: generate.serviceName({ prefix: 'stream' }),
      topicName2: generate.serviceName({ prefix: 'topic' }),
    };

    cy.log('add elements:');
    cy.log(`element: perf source ${elements.perfSourceName}`);
    cy.log(`element: ftp source ${elements.ftpSourceName}`);
    cy.log(`element: console sink ${elements.consoleSinkName}`);
    cy.log(`element: hdfs sink ${elements.hdfsSinkName}`);
    cy.log(`element: shared topic 1 ${elements.topicName1}`);
    cy.log(`element: stream ${elements.streamName}`);
    cy.log(`element: shared topic 2 ${elements.topicName2}`);
    cy.addElement(elements.perfSourceName, KIND.source, connectorSources.perf);
    cy.addElement(elements.ftpSourceName, KIND.source, connectorSources.ftp);
    cy.addElement(elements.consoleSinkName, KIND.sink, connectorSinks.console);
    cy.addElement(elements.hdfsSinkName, KIND.sink, connectorSinks.hdfs);
    cy.addElement(elements.topicName1, KIND.topic, null);
    cy.addElement(elements.streamName, KIND.stream, null);
    cy.addElement(elements.topicName2, KIND.topic, null);

    // let the backend API makes effect
    cy.wait(5000);

    // to get the actual data-testid
    // we need to refresh the paper again
    cy.reload();

    // 1. perf source -> topic1 -> stream -> topic2 -> hdfs sink
    cy.log(
      `create a link from perf source to topic 1 to stream to topic 2 to hdfs sink`,
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
    cy.get('g[data-type="standard.Link"]').should('have.length', 4);

    // 2. we don't allow multiple links for same cell
    // perf source -> console sink
    cy.log(`cannot create a link from perf source to console sink`);
    cy.getCell(elements.perfSourceName).trigger('mouseover');
    cy.cellAction(elements.perfSourceName, ACTIONS.link).click();
    cy.getCell(elements.consoleSinkName).click();
    cy.findByText(
      `The source ${elements.perfSourceName} is already connected to a target`,
    )
      .should('exist')
      .siblings('div')
      .first()
      .click();

    // ftp source -> stream
    cy.log(`cannot create a link from ftp source to stream`);
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

    // clear the global topics
    topics = [];
  });
});

describe('Topic Operations of Pipeline', () => {
  before(async () => await deleteAllServices());
  it('connect two elements of pipeline should auto generate pipeline-only topic', () => {
    cy.createWorkspace();

    // Add new pipeline
    cy.findByText(/^pipelines$/i)
      .siblings('svg')
      .first()
      .click()
      .findByText(/^add a new pipeline$/i)
      .should('exist');

    cy.findByTestId('new-pipeline-dialog')
      .find('input')
      .type('pipeline1');

    cy.findByText(/^add$/i).click();

    // force to reload the page in order to get the correct data in toolbox
    cy.reload();

    // check the toolbox
    cy.findByText(/^toolbox$/i).should('exist');

    const elements = {
      perfSourceName: generate.serviceName({ prefix: 'source' }),
      consoleSinkName: generate.serviceName({ prefix: 'sink' }),
      ftpSourceName: generate.serviceName({ prefix: 'source' }),
      hdfsSinkName: generate.serviceName({ prefix: 'sink' }),
      streamName1: generate.serviceName({ prefix: 'stream' }),
      streamName2: generate.serviceName({ prefix: 'stream' }),
      streamName3: generate.serviceName({ prefix: 'stream' }),
    };

    cy.log('add elements:');
    cy.log(`element: perf source ${elements.perfSourceName}`);
    cy.log(`element: console sink ${elements.consoleSinkName}`);
    cy.log(`element: ftp source ${elements.ftpSourceName}`);
    cy.log(`element: hdfs sink ${elements.hdfsSinkName}`);
    cy.log(`element: stream1 ${elements.streamName1}`);
    cy.log(`element: stream2 ${elements.streamName2}`);
    cy.log(`element: stream3 ${elements.streamName3}`);
    cy.addElement(elements.perfSourceName, KIND.source, connectorSources.perf);
    cy.addElement(elements.consoleSinkName, KIND.sink, connectorSinks.console);
    cy.addElement(elements.ftpSourceName, KIND.source, connectorSources.ftp);
    cy.addElement(elements.hdfsSinkName, KIND.sink, connectorSinks.hdfs);
    cy.addElement(elements.streamName1, KIND.stream, null);
    cy.addElement(elements.streamName2, KIND.stream, null);
    cy.addElement(elements.streamName3, KIND.stream, null);

    // let the backend API makes effect
    cy.wait(5000);

    // to get the actual data-testid
    // we need to refresh the paper again
    cy.reload();

    cy.log(`auto generate topic for perf source -> console sink`);
    cy.getCell(elements.perfSourceName).trigger('mouseover');
    cy.cellAction(elements.perfSourceName, ACTIONS.link).click();
    cy.getCell(elements.consoleSinkName).click();

    // generate "T1" pipeline-only topic
    cy.findAllByText('T1').should('exist');
    // will create two lines
    cy.get('g[data-type="standard.Link"]').should('have.length', 2);

    // topic creation is a heavy request...we need to wait util ready
    cy.wait(5000);

    cy.log(`auto generate topic for ftp source -> stream1`);
    cy.getCell(elements.ftpSourceName).trigger('mouseover');
    cy.cellAction(elements.ftpSourceName, ACTIONS.link).click();
    cy.getCell(elements.streamName1).click();

    // generate "T2" pipeline-only topic
    cy.findAllByText('T2').should('exist');
    // will create two lines (total four lines)
    cy.get('g[data-type="standard.Link"]').should('have.length', 4);

    // topic creation is a heavy request...we need to wait util ready
    cy.wait(5000);

    cy.log(`auto generate topic for stream2 -> stream3`);
    cy.getCell(elements.streamName2).trigger('mouseover');
    cy.cellAction(elements.streamName2, ACTIONS.link).click();
    cy.getCell(elements.streamName3).click();

    // generate "T3" pipeline-only topic
    cy.findAllByText('T3').should('exist');
    // will create two lines (total six lines)
    cy.get('g[data-type="standard.Link"]').should('have.length', 6);

    // topic creation is a heavy request...we need to wait util ready
    cy.wait(5000);

    // check pipeline-only topics will be shown in topic list
    cy.contains('button', 'workspace', { matchCase: false })
      .should('exist')
      .click();

    cy.findByText(/^topics$/i)
      .should('exist')
      .click();

    cy.contains('td', 'T1').should('exist');
    cy.contains('td', 'T2').should('exist');
    cy.contains('td', 'T3').should('exist');

    cy.findAllByText(/pipeline only/i)
      .filter(':visible')
      .should('have.length', 3);
  });
});
