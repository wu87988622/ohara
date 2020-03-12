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

    // wait a little time for the toolbox list rendered
    cy.wait(2000);

    if (kind !== KIND.topic && kind !== KIND.stream) {
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
      //TODO
      cy.end();
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

describe('Pipeline Page', () => {
  beforeEach(async () => await deleteAllServices());

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

    // force to reload the page in order to get the connectors
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

    cy.log('add elements:');
    cy.log(`element: perf source ${elements.perfSourceName}`);
    cy.log(`element: ftp source ${elements.ftpSourceName}`);
    cy.log(`element: pipeline-only topic 1 ${elements.pipelineOnlyTopicName1}`);
    cy.log(`element: console sink ${elements.consoleSinkName}`);
    cy.log(`element: pipeline-only topic 2 ${elements.pipelineOnlyTopicName2}`);
    cy.log(`element: hdfs sink ${elements.hdfsSinkName}`);
    cy.addElement(elements.perfSourceName, KIND.source, connectorSources.perf);
    cy.addElement(elements.ftpSourceName, KIND.source, connectorSources.ftp);
    cy.addElement(elements.pipelineOnlyTopicName1, KIND.topic, null);
    cy.addElement(elements.consoleSinkName, KIND.sink, connectorSinks.console);
    cy.addElement(elements.pipelineOnlyTopicName2, KIND.topic, null);
    cy.addElement(elements.hdfsSinkName, KIND.sink, connectorSinks.hdfs);

    // let the backend API makes effect
    cy.wait(5000);

    // to get the actual data-testid we need to refresh the paper again
    cy.reload();

    // 1. perf source -> ftp source
    cy.log(`cannot create a link from perf source to ftp source`);
    cy.getCell(elements.perfSourceName).trigger('mouseover');
    cy.cellAction(elements.perfSourceName, ACTIONS.link).click();
    cy.getCell(elements.ftpSourceName).click();
    cy.findByText(`Target ${elements.ftpSourceName} is a source!`)
      .should('exist')
      .siblings('div')
      .click();

    // 2. pipeline-only topic 1 -> pipeline-only topic 2
    cy.log(`cannot create a link between pipeline-only topic 1 and 2`);
    cy.getCell(elements.pipelineOnlyTopicName1).trigger('mouseover');
    cy.cellAction(elements.pipelineOnlyTopicName1, ACTIONS.link).click();
    cy.getCell(elements.pipelineOnlyTopicName2).click();
    cy.findByText(
      `Cannot connect a ${KIND.topic} to another ${KIND.topic}, they both have the same type`,
    )
      .should('exist')
      .siblings('div')
      .click();

    // 3. pipeline-only topic 1 -> hdfs sink
    cy.log(
      `create a link from a perf source to pipeline-only topic 1 to hdfs sink`,
    );
    cy.getCell(elements.perfSourceName).trigger('mouseover');
    cy.cellAction(elements.perfSourceName, ACTIONS.link).click();
    cy.getCell(elements.pipelineOnlyTopicName1).click();
    cy.getCell(elements.pipelineOnlyTopicName1).trigger('mouseover');
    cy.cellAction(elements.pipelineOnlyTopicName1, ACTIONS.link).click();
    cy.getCell(elements.hdfsSinkName).click();
    // will create two links
    cy.get('g[data-type="standard.Link"]').should('have.length', 2);

    // 4. ftp source -> hdfs sink
    cy.log(`cannot create a link from ftp source to hdfs sink`);
    cy.getCell(elements.ftpSourceName).trigger('mouseover');
    cy.cellAction(elements.ftpSourceName, ACTIONS.link).click();
    cy.getCell(elements.hdfsSinkName).click();
    cy.findByText(
      `The target ${elements.hdfsSinkName} is already connected to a source`,
    )
      .should('exist')
      .siblings('div')
      .click();

    // 5. pipeline-only topic 2 -> hdfs sink
    cy.log(`cannot create a link from pipeline-only topic to hdfs sink`);
    cy.getCell(elements.pipelineOnlyTopicName2).trigger('mouseover');
    cy.cellAction(elements.pipelineOnlyTopicName2, ACTIONS.link).click();
    cy.getCell(elements.hdfsSinkName).click();
    cy.findByText(
      `The target ${elements.hdfsSinkName} is already connected to a source`,
    )
      .should('exist')
      .siblings('div')
      .click();

    // we can force delete a connected topic
    cy.log(`force delete pipeline-only topic`);
    cy.getCell(elements.pipelineOnlyTopicName1).trigger('mouseover');
    cy.cellAction(elements.pipelineOnlyTopicName1, ACTIONS.remove).click();
    cy.findByText(/^delete$/i).should('exist');
    cy.contains('span:visible', /cancel/i).click();
    cy.findAllByText(elements.pipelineOnlyTopicName1).should('exist');

    // delete all elements
    Object.values(elements).forEach(element => {
      // the private topic was removed in the previous steps, so skip it here
      if (element === elements.privateTopicName) return;
      cy.getCell(element).trigger('mouseover');
      cy.cellAction(element, ACTIONS.remove).click();
      cy.wait(1000);
      cy.findByText(/^delete$/i).click();
      cy.findAllByText(element)
        .filter(':visible')
        .should('not.exist');
    });

    // clear the global topic variable for next retries
    topics = [];
  });
});
