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
import * as generate from '../../src/utils/generate';

describe('PipelineEditPage', () => {
  before(() => {
    cy.removeWorkers();
    cy.addWorker();
  });

  beforeEach(() => {
    cy.server();
    cy.route('POST', 'api/pipelines').as('postPipeline');
    cy.route('GET', 'api/pipelines/*').as('getPipeline');
    cy.route('PUT', 'api/pipelines/*').as('putPipeline');
    cy.route('GET', 'api/topics').as('getTopics');
    cy.route('GET', 'api/workers').as('getWorkers');
    cy.route('POST', '/api/connectors').as('postConnector');
    cy.route('GET', '/api/connectors/*').as('getConnector');
    cy.route('PUT', '/api/connectors/*').as('putConnector');

    const pipelineName = generate.serviceName({ prefix: 'pipeline' });

    cy.addTopic();
    cy.visit(URLS.PIPELINES)
      .findByTestId('new-pipeline')
      .click()
      .findByTestId('pipeline-name-input')
      .type(pipelineName)
      .findByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('WORKER_NAME')}]`)
      .click()
      .findByText('ADD')
      .click()
      .wait('@getPipeline');
  });

  it('adds a topic into pipeline graph and removes it later', () => {
    // Add the topic
    cy.wait('@getTopics')
      .findByTestId('toolbar-topics')
      .click()
      .findByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .findByText('ADD')
      .click()
      .wait('@putPipeline')
      .findByText(Cypress.env('TOPIC_NAME'))
      .should('be.exist');

    // Remove the topic
    cy.findByText(Cypress.env('TOPIC_NAME'))
      .click({ force: true })
      .findByTestId('delete-button')
      .click()
      .findByText('DELETE')
      .click()
      .findByText(
        `Successfully deleted the topic: ${Cypress.env('TOPIC_NAME')}`,
      )
      .wait('@getPipeline')
      .wait(500) // wait here, so the local state is up-to-date with the API response
      .queryByText(Cypress.env('TOPIC_NAME'), { timeout: 500 })
      .should('not.be.exist');
  });

  it('should prevent user from deleting a topic that has connection', () => {
    // Add topic
    cy.wait('@getTopics')
      .findByTestId('toolbar-topics')
      .click()
      .findByTestId('topic-select')
      .findByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .findByText('ADD')
      .click()
      .wait('@putPipeline');

    // Add ftp source connector
    const connectorName = generate.serviceName({ prefix: 'connector' });

    cy.findByTestId('toolbar-sources')
      .click()
      .findByText(CONNECTOR_TYPES.jdbcSource)
      .click()
      .findByText('ADD')
      .click()
      .findByPlaceholderText('myconnector')
      .type(connectorName)
      .findByTestId('new-connector-dialog')
      .within(() => {
        cy.findByText('ADD').click();
      })
      .wait('@putPipeline');

    // Set the connection between them
    cy.findByText(connectorName)
      .click({ force: true })
      .wait('@getConnector')
      .findByText('core')
      .click({ force: true })
      .findByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .wait(2000) // UI has one sec throttle, so we need to wait a bit time and then wait for the request
      .wait('@putPipeline');

    cy.wait(1000); // Temp workaround, need this to make sure Cypress can get the right element to work on

    // Try to remove the topic
    cy.findByTestId('pipeline-graph')
      .within(() => {
        cy.findByText(Cypress.env('TOPIC_NAME')).click({ force: true });
      })
      .findByTestId('delete-button')
      .click()
      .findByTestId('delete-dialog')
      .within(() => {
        cy.findByText('DELETE').click();
      });

    cy.findByText('You cannot delete the topic while it has any connection');
    // The topic should still be there
    cy.findByTestId('pipeline-graph').within(() =>
      cy.findByText(Cypress.env('TOPIC_NAME')),
    );
  });

  it('adds and deletes all connectors', () => {
    const connectors = [
      {
        type: CONNECTOR_TYPES.ftpSource,
        nodeType: 'FtpSource',
        toolbarTestId: 'toolbar-sources',
        connectorName: generate.serviceName({ prefix: 'connector' }),
      },
      {
        type: CONNECTOR_TYPES.jdbcSource,
        nodeType: 'JDBCSourceConnector',
        toolbarTestId: 'toolbar-sources',
        connectorName: generate.serviceName({ prefix: 'connector' }),
      },
      {
        type: CONNECTOR_TYPES.ftpSink,
        nodeType: 'FtpSink',
        toolbarTestId: 'toolbar-sinks',
        connectorName: generate.serviceName({ prefix: 'connector' }),
      },
      {
        type: CONNECTOR_TYPES.hdfsSink,
        nodeType: 'HDFSSink',
        toolbarTestId: 'toolbar-sinks',
        connectorName: generate.serviceName({ prefix: 'connector' }),
      },
    ];

    // Add connector to the graph
    cy.wrap(connectors).each(connector => {
      const { toolbarTestId, type, nodeType, connectorName } = connector;
      cy.findByTestId(toolbarTestId)
        .click()
        .findByText(type)
        .click()
        .findByText('ADD')
        .click()
        .findByPlaceholderText('myconnector')
        .type(connectorName)
        .findByTestId('new-connector-dialog')
        .within(() => {
          cy.findByText('ADD').click();
        });

      cy.wait(['@postConnector', '@putPipeline'])
        .getAllByText(connectorName)
        .should('have.length', 1)
        .get('.node-type')
        .should('contain', nodeType);
    });

    // Remove connectors from the graph
    cy.wrap(connectors).each(connector => {
      const { connectorName } = connector;

      cy.findByText(connectorName)
        .click({ force: true })
        .findByTestId('delete-button')
        .click()
        .findByText('DELETE')
        .click()
        .findByText(`Successfully deleted the connector: ${connectorName}`)
        .wait('@getPipeline')
        .wait(500) // wait here, so the local state is up-to-date with the API response
        .queryByText(connectorName, { timeout: 500 })
        .should('not.be.exist');
    });
  });

  it('saves and removes a connector even after page refresh', () => {
    const connectorName = generate.serviceName({ prefix: 'connector' });
    cy.findByTestId('toolbar-sources')
      .click()
      .findByText(CONNECTOR_TYPES.jdbcSource)
      .click()
      .findByText('ADD')
      .click()
      .findByPlaceholderText('myconnector')
      .type(connectorName)
      .findByTestId('new-connector-dialog')
      .within(() => {
        cy.findByText('ADD').click();
      })
      .findByText(connectorName)
      .should('have.length', '1')
      .get('.node-type')
      .should('contain', 'JDBCSourceConnector')
      .wait(3000)
      .reload()
      .findByText(connectorName)
      .should('have.length', '1')
      .get('.node-type')
      .should('contain', 'JDBCSourceConnector');

    cy.findByText(connectorName)
      .click({ force: true })
      .findByTestId('delete-button')
      .click()
      .findByText('DELETE')
      .click()
      .findByText(`Successfully deleted the connector: ${connectorName}`)
      .should('be.exist')
      .wait(3000)
      .reload()
      .queryByText(connectorName, { timeout: 500 })
      .should('not.be.exist')
      .get('.node-type')
      .should('not.be.exist');
  });

  it('connects Ftp soure -> Topic -> Ftp sink', () => {
    cy.findByTestId('toolbar-sinks')
      .click()
      .findByText(CONNECTOR_TYPES.ftpSink)
      .click()
      .findByText('ADD')
      .click()
      .findByPlaceholderText('myconnector')
      .type(generate.serviceName({ prefix: 'connector' }))
      .findByTestId('new-connector-dialog')
      .within(() => {
        cy.findByText('ADD').click();
      })
      .wait('@putPipeline')
      .findByTestId('toolbar-sources')
      .click()
      .findByText(CONNECTOR_TYPES.ftpSource)
      .click()
      .findByText('ADD')
      .click()
      .findByPlaceholderText('myconnector')
      .type(generate.serviceName({ prefix: 'connector' }))
      .findByTestId('new-connector-dialog')
      .within(() => {
        cy.findByText('ADD').click();
      })
      .wait('@putPipeline')
      .findByTestId('toolbar-topics')
      .click()
      .findByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .findByText('ADD')
      .click()
      .wait('@putPipeline');

    cy.findByText('FtpSink')
      .click({ force: true })
      .findByText('core')
      .click()
      .wait('@getConnector')
      .findByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .wait(2000) // UI has one sec throttle, so we need to wait a bit time and then wait for the request
      .wait('@putPipeline')
      .get('g.edgePath')
      .should('have.length', 1);

    cy.wait(1000); // Temp workaround, need this to make sure Cypress can get the right element to work on

    cy.findByText('FtpSource')
      .click({ force: true })
      .wait('@getConnector')
      .findByText('core')
      .click({ force: true })
      .findByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .wait(2000) // UI has one sec throttle, so we need to wait a bit time and then wait for the request
      .wait('@putPipeline')
      .get('g.edgePath')
      .should('have.length', 2);
  });

  it('connects Jdbc source -> Topic -> Hdfs sink together', () => {
    cy.findByTestId('toolbar-sinks')
      .click()
      .findByText(CONNECTOR_TYPES.hdfsSink)
      .click()
      .findByText('ADD')
      .click()
      .findByPlaceholderText('myconnector')
      .type(generate.serviceName({ prefix: 'connector' }))
      .findByTestId('new-connector-dialog')
      .within(() => {
        cy.findByText('ADD').click();
      })
      .wait('@putPipeline')
      .findByTestId('toolbar-sources')
      .click()
      .findByText(CONNECTOR_TYPES.jdbcSource)
      .click()
      .findByText('ADD')
      .click()
      .findByPlaceholderText('myconnector')
      .type(generate.serviceName({ prefix: 'connector' }))
      .findByTestId('new-connector-dialog')
      .within(() => {
        cy.findByText('ADD').click();
      })
      .wait('@putPipeline')
      .findByTestId('toolbar-topics')
      .click()
      .findByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .findByText('ADD')
      .click()
      .wait('@putPipeline');

    cy.wait(1000); // Temp workaround, need this to make sure Cypress can get the right element to work on

    cy.findByText('HDFSSink')
      .click({ force: true })
      .wait('@getConnector')
      .findByText('core')
      .click()
      .findByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .wait(2000) // UI has one sec throttle, so we need to wait a bit time and then wait for the request
      .wait('@putPipeline')
      .get('g.edgePath')
      .should('have.length', 1);

    cy.wait(1000); // Temp workaround, need this to make sure Cypress can get the right element to work on

    cy.findByText('JDBCSourceConnector')
      .click({ force: true })
      .wait('@getConnector')
      .findByText('core')
      .click()
      .findByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .wait(2000) // UI has one sec throttle, so we need to wait a bit time and then wait for the request
      .wait('@putPipeline')
      .get('g.edgePath')
      .should('have.length', 2);
  });

  it('connects perf source -> Topic', () => {
    const perfName = generate.serviceName({ prefix: 'connector' });
    const topicName = Cypress.env('TOPIC_NAME');

    cy.findByTestId('toolbar-sources')
      .click()
      .findByText('Add a new source connector')
      .should('be.exist')
      .findByText(CONNECTOR_TYPES.perfSource)
      .click()
      .findByText('ADD')
      .click()
      .findByPlaceholderText('myconnector')
      .type(perfName)
      .findByTestId('new-connector-dialog')
      .within(() => {
        cy.findByText('ADD').click();
      })
      .wait('@putPipeline')
      .findByTestId('toolbar-topics')
      .click()
      .findByText('Please select...')
      .click()
      .get(`li[data-value=${Cypress.env('TOPIC_NAME')}]`)
      .click()
      .findByText('ADD')
      .click()
      .wait('@putPipeline')
      .findByText('PerfSource')
      .click({ force: true })
      .findByText('core')
      .click()
      .findByText('Please select...')
      .click()
      .get(`li[data-value=${topicName}]`)
      .click()
      .wait(2000)
      .wait('@putPipeline')
      .findByTestId('start-btn')
      .click({ force: true })
      .wait('@putConnector')
      .wait('@getPipeline') // Waiting for multiple `GET pipeline` requests here, so the metrics data can be ready for us to test
      .wait('@getPipeline')
      .wait('@getPipeline')
      .wait('@getPipeline')
      .wait('@getPipeline')
      .findByText(`Metrics (${perfName})`)
      .should('have.length', 1)
      .get('@getPipeline')
      .then(xhr => {
        const perf = xhr.response.body.objects.find(
          object => object.name === perfName,
        );

        const metricsCount = perf.metrics.meters.length;
        cy.get('[data-testid="metric-item"]').should(
          'have.length',
          metricsCount,
        );
      })
      // Element is effective width and height of 0x0, even though it is not.
      // reference https://github.com/cypress-io/cypress/issues/695
      // 1.We should be able to write an assertion that checks on non-zero width  where the tests will not move forward until it is greater than non-zero
      // reference https://github.com/cypress-io/cypress/issues/695#issuecomment-379817133
      // 2.Topic click cause refresh cypress couldn't get DOM event so click unable to end, we need click two time.
      // reference https://github.com/cypress-io/cypress/issues/695#issuecomment-493578023
      .get('@getPipeline')
      .findByTestId(`topic-${topicName}`)
      .invoke('width')
      .should('be.gt', 0)
      .findByTestId(`topic-${topicName}`)
      .click({ force: true })
      .findByTestId(`topic-${topicName}`)
      .click({ force: true })

      .findByText(`Metrics (${topicName})`)
      .should('have.length', 1)
      .findByTestId('expandIcon')
      .click({ force: true })
      .get('@getPipeline')
      .then(xhr => {
        const topic = xhr.response.body.objects.find(
          object => object.name === topicName,
        );

        const metricsCount = topic.metrics.meters.length;
        cy.get('[data-testid="metric-item"]').should(
          'have.length',
          metricsCount,
        );
      });
  });
});
