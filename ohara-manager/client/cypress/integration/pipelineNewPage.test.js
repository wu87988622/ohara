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

import * as URLS from '../../src/constants/urls';
import { CONNECTOR_TYPES } from '../../src/constants/pipelines';
import { makeRandomStr } from '../utils';

describe('PipelineNewPage', () => {
  before(() => {
    cy.deleteAllWorkers();
    cy.createWorker();
  });

  beforeEach(() => {
    cy.server();
    cy.route('GET', 'api/pipelines/*').as('getPipeline');
    cy.route('PUT', 'api/pipelines/*').as('putPipeline');
    cy.route('POST', 'api/pipelines').as('postPipeline');
    cy.route('GET', 'api/topics').as('getTopics');
    cy.route('GET', 'api/workers').as('getWorkers');

    const pipelineName = makeRandomStr();

    cy.createTopic().as('createTopic');
    cy.visit(URLS.PIPELINE)
      .getByTestId('new-pipeline')
      .click()
      .getByLabelText('Pipeline name')
      .click()
      .type(pipelineName)
      .wait('@getWorkers')
      .getByText('Add')
      .click()
      .wait('@getPipeline');
  });

  it('adds a topic into pipeline graph and removes it later', () => {
    // Add the topic
    cy.wait('@getTopics')
      .getByTestId('toolbar-topics')
      .click()
      .get('@createTopic')
      .then(topic => {
        cy.getByTestId('topic-select').select(topic.name);
      })
      .getByText('Add')
      .click()
      .wait('@putPipeline')
      .get('@createTopic')
      .then(topic => {
        cy.getByText(topic.name).should('be.exist');
      });

    // Remove the topic
    cy.get('@createTopic').then(topic => {
      cy.getByText(topic.name)
        .click({ force: true })
        .getByTestId('delete-button')
        .click()
        .getByText('Yes, Remove this topic')
        .click()
        .getByText(`Successfully deleted the topic: ${topic.name}`)
        .wait('@getPipeline')
        .queryByText(topic.name, { timeout: 500 })
        .should('not.be.exist');
    });
  });

  it('adds all connectors', () => {
    const filters = [
      {
        type: CONNECTOR_TYPES.ftpSource,
        nodeType: 'FtpSource',
        toolbarTestId: 'toolbar-sources',
        connectorName: makeRandomStr(),
      },
      {
        type: CONNECTOR_TYPES.jdbcSource,
        nodeType: 'JDBCSourceConnector',
        toolbarTestId: 'toolbar-sources',
        connectorName: makeRandomStr(),
      },
      {
        type: CONNECTOR_TYPES.ftpSink,
        nodeType: 'FtpSink',
        toolbarTestId: 'toolbar-sinks',
        connectorName: makeRandomStr(),
      },
      {
        type: CONNECTOR_TYPES.hdfsSink,
        nodeType: 'HDFSSinkConnector',
        toolbarTestId: 'toolbar-sinks',
        connectorName: makeRandomStr(),
      },
    ];

    cy.server();
    cy.route('POST', '/api/connectors').as('createConnector');

    cy.wrap(filters).each(filter => {
      const { toolbarTestId, type, nodeType, connectorName } = filter;
      cy.getByTestId(toolbarTestId).click();
      cy.getByText(type)
        .click()
        .getByText('Add')
        .click()
        .getByPlaceholderText('Connector name')
        .type(connectorName)
        .get('.ReactModal__Content')
        .eq(1)
        .within(() => {
          cy.getByText('Add').click();
        });

      cy.wait('@createConnector')
        .getAllByText(connectorName)
        .should('have.length', 1)
        .get('.node-type')
        .should('contain', nodeType);
    });
  });

  it('saves and removes a connector even after page refresh', () => {
    const connectorName = makeRandomStr();
    cy.getByTestId('toolbar-sources')
      .click()
      .getByText(CONNECTOR_TYPES.jdbcSource)
      .click()
      .getByText('Add')
      .click()
      .getByPlaceholderText('Connector name')
      .type(connectorName)
      .get('.ReactModal__Content')
      .eq(1)
      .within(() => {
        cy.getByText('Add').click();
      })
      .getByText(connectorName)
      .should('have.length', '1')
      .get('.node-type')
      .should('contain', 'JDBCSourceConnector')
      .wait(3000)
      .reload()
      .getByText(connectorName)
      .should('have.length', '1')
      .get('.node-type')
      .should('contain', 'JDBCSourceConnector');

    cy.getByText(connectorName)
      .click()
      .getByTestId('delete-button')
      .click()
      .getByText('Yes, Remove this connector')
      .click()
      .getByText(`Successfully deleted the connector: ${connectorName}`)
      .should('be.exist')
      .wait(3000)
      .reload()
      .queryByText(connectorName, { timeout: 500 })
      .should('not.be.exist')
      .get('.node-type')
      .should('not.be.exist');
  });

  it('connects Ftp soure -> Topic -> Ftp sink', () => {
    cy.server();
    cy.route('PUT', 'api/pipelines/*').as('putPipeline');
    cy.route('GET', 'api/connectors/*').as('getConnector');
    cy.route('GET', 'api/workers').as('getWorkers');

    cy.getByTestId('toolbar-sinks')
      .click()
      .getByText('Add a new sink connector')
      .should('be.exist')
      .getByText(CONNECTOR_TYPES.ftpSink)
      .click()
      .getByText('Add')
      .click()
      .getByPlaceholderText('Connector name')
      .type(makeRandomStr())
      .get('.ReactModal__Content')
      .eq(1)
      .within(() => {
        cy.getByText('Add').click();
      })
      .wait('@putPipeline')
      .getByTestId('toolbar-sources')
      .click()
      .getByText('Add a new source connector')
      .should('be.exist')
      .getByText(CONNECTOR_TYPES.ftpSource)
      .click()
      .getByText('Add')
      .click()
      .getByPlaceholderText('Connector name')
      .type(makeRandomStr())
      .get('.ReactModal__Content')
      .eq(1)
      .within(() => {
        cy.getByText('Add').click();
      })
      .wait('@putPipeline')
      .getByTestId('toolbar-topics')
      .click()
      .get('@createTopic')
      .then(topic => {
        cy.getByTestId('topic-select').select(topic.name);
      })
      .getByText('Add')
      .click()
      .wait('@putPipeline');

    cy.getByText('FtpSink')
      .click()
      .getByText('FTP sink connector')
      .should('have.length', '1')
      .getByText('core')
      .click()
      .wait('@getConnector')
      .get('@createTopic')
      .then(topic => {
        cy.getByText('Please select...')
          .click()
          .get(`li[data-value=${topic.name}]`)
          .click();
      })
      .wait('@putPipeline')
      .get('g.edgePath')
      .should('have.length', 1);

    cy.getByText('FtpSource')
      .click()
      .getByText('FTP source connector')
      .should('have.length', '1')
      .wait('@getConnector')
      .getByText('core')
      .click({ force: true })
      .get('@createTopic')
      .then(topic => {
        cy.getByText('Please select...')
          .click()
          .get(`li[data-value=${topic.name}]`)
          .click();
      })
      .wait('@putPipeline')
      .get('g.edgePath')
      .should('have.length', 2);
  });

  it('connects Jdbc source -> Topic -> Hdfs sink together', () => {
    cy.server();
    cy.route('PUT', '/api/pipelines/*').as('putPipeline');
    cy.route('GET', '/api/connectors/*').as('getConnector');

    cy.getByTestId('toolbar-sinks')
      .click()
      .getByText('Add a new sink connector')
      .should('be.exist')
      .getByText(CONNECTOR_TYPES.hdfsSink)
      .click()
      .getByText('Add')
      .click()
      .getByPlaceholderText('Connector name')
      .type(makeRandomStr())
      .get('.ReactModal__Content')
      .eq(1)
      .within(() => {
        cy.getByText('Add').click();
      })
      .wait('@putPipeline')
      .getByTestId('toolbar-sources')
      .click()
      .getByText('Add a new source connector')
      .should('be.exist')
      .getByText(CONNECTOR_TYPES.jdbcSource)
      .click()
      .getByText('Add')
      .click()
      .getByPlaceholderText('Connector name')
      .type(makeRandomStr())
      .get('.ReactModal__Content')
      .eq(1)
      .within(() => {
        cy.getByText('Add').click();
      })
      .wait('@putPipeline')
      .getByTestId('toolbar-topics')
      .click()
      .get('@createTopic')
      .then(topic => {
        cy.getByTestId('topic-select').select(topic.name);
      })
      .getByText('Add')
      .click()
      .wait('@putPipeline');

    cy.getByText('HDFSSinkConnector')
      .click()
      .wait('@getConnector')
      .getByText('HDFS sink connector')
      .should('have.length', '1')
      .getByText('core')
      .click()
      .get('@createTopic')
      .then(topic => {
        cy.getByText('Please select...')
          .click()
          .get(`li[data-value=${topic.name}]`)
          .click();
      })
      .wait('@putPipeline')
      .get('g.edgePath')
      .should('have.length', 1);

    cy.getByText('JDBCSourceConnector')
      .click()
      .wait('@getConnector')
      .getByText('JDBC source connector')
      .should('have.length', '1')
      .getByText('core')
      .click()
      .get('@createTopic')
      .then(topic => {
        cy.getByText('Please select...')
          .click()
          .get(`li[data-value=${topic.name}]`)
          .click();
      })
      .wait('@putPipeline')
      .get('g.edgePath')
      .should('have.length', 2);
  });
});
