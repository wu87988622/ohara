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

import { WORKSPACES } from '../../src/constants/urls';
import { CONNECTOR_FILTERS } from '../../src/constants/pipelines';
import { divide, floor } from 'lodash';
import * as generate from '../../src/utils/generate';

describe('WorkspacesPage', () => {
  before(() => {
    cy.removeWorkers();
    cy.addWorker();
  });

  beforeEach(() => {
    cy.server();
    cy.route('GET', 'api/workers').as('getWorkers');
    cy.route('GET', 'api/workers/*').as('getWorker');
    cy.route('GET', 'api/brokers/*').as('getBroker');
    cy.route('GET', 'api/zookeepers/*').as('getZookeeper');
    cy.route('GET', 'api/topics?*').as('getTopics');
    cy.route('GET', 'api/files?*').as('getFiles');
    cy.route('POST', 'api/zookeepers').as('createZookeeper');
    cy.route('POST', 'api/brokers').as('createBroker');
  });

  it('creates a workspace', () => {
    const nodeName = Cypress.env('nodeHost');
    const workerName = generate.serviceName({ prefix: 'worker' });

    cy.registerService(workerName, 'workers');

    cy.visit(WORKSPACES)
      .getByText('New workspace')
      .click()
      .getByPlaceholderText('cluster00')
      .type(workerName)
      .getByTestId(nodeName)
      .click()
      .uploadJar(
        'input[type=file]',
        'plugin/ohara-it-sink.jar',
        'ohara-it-sink.jar',
        'application/java-archive',
      )
      .wait(500)
      .getByText('ohara-it-sink')
      .click()
      .getByText('Add')
      .click()
      .wait('@createZookeeper')
      .then(xhr => {
        const { name } = xhr.response.body;
        cy.registerService(name, 'zookeepers');
      })
      .wait('@createBroker')
      .then(xhr => {
        const { name } = xhr.response.body;
        cy.registerService(name, 'brokers');
      });

    cy.getByText(workerName).should('have.length', 1);
  });

  it('adds a new topic', () => {
    const topicName = generate.serviceName({ prefix: 'topic' });

    cy.visit(WORKSPACES)
      .wait('@getWorkers')
      .getByTestId(Cypress.env('WORKER_NAME'))
      .click()
      .getByTestId('workspace-tab')
      .within(() => {
        cy.getByText('Topics').click();
      })
      .getByText('New topic')
      .click()
      .getByPlaceholderText('Kafka Topic')
      .type(topicName)
      .getByTestId('partitions-input')
      .type(1)
      .getByTestId('replications-input')
      .type(1)
      .getByText('Add')
      .click()
      .wait('@getTopics')
      .getByText(topicName)
      .should('have.length', 1);
  });

  it('deletes a topic', () => {
    cy.addTopic();

    cy.visit(WORKSPACES)
      .wait('@getWorkers')
      .getByTestId(Cypress.env('WORKER_NAME'))
      .click()
      .getByTestId('workspace-tab')
      .within(() => {
        cy.getByText('Topics').click();
      })
      .wait('@getTopics');

    cy.getByTestId(Cypress.env('TOPIC_NAME'))
      .click({ force: true })
      .getByText('DELETE')
      .click()
      .getByText(`Successfully deleted the topic: ${Cypress.env('TOPIC_NAME')}`)
      .should('have.length', 1);
  });

  it('adds and removes a new streamApp', () => {
    cy.visit(WORKSPACES)
      .wait('@getWorkers')
      .getByTestId(Cypress.env('WORKER_NAME'))
      .click()
      .getByTestId('workspace-tab')
      .within(() => {
        cy.getByText('Stream jars').click();
      })
      .wait('@getFiles')
      .uploadJar(
        'input[type=file]',
        'streamApp/ohara-streamapp.jar',
        'ohara-streamapp.jar',
        'application/java-archive',
      )
      .wait(500)
      .getByText('ohara-streamapp.jar')
      .should('have.length', 1);

    cy.getByTestId('ohara-streamapp.jar')
      .click()
      .getByText('DELETE')
      .click()
      .getByText('Successfully deleted the stream app!')
      .should('have.length', 1)
      .wait('@getFiles')
      .get('td')
      .should('have.length', 0);
  });

  it('should warn the user when a duplicate jar name is upload', () => {
    cy.visit(WORKSPACES)
      .wait('@getWorkers')
      .getByTestId(Cypress.env('WORKER_NAME'))
      .click()
      .getByTestId('workspace-tab')
      .within(() => {
        cy.getByText('Stream jars').click();
      })
      .wait('@getFiles')
      .uploadJar(
        'input[type=file]',
        'streamApp/ohara-streamapp.jar',
        'ohara-streamapp.jar',
        'application/java-archive',
      )
      .wait(500)
      .uploadJar(
        'input[type=file]',
        'streamApp/ohara-streamapp.jar',
        'ohara-streamapp.jar',
        'application/java-archive',
      )
      .wait(500)
      .getByText('The jar name already exists!')
      .should('have.length', 1)
      .queryAllByText('ohara-streamapp.jar')
      .should('have.length', 1);
  });

  it('should link to the correct service page', () => {
    cy.visit(WORKSPACES)
      .wait('@getWorkers')
      .getByTestId(Cypress.env('WORKER_NAME'))
      .click()
      .url()
      .should('include', '/overview')
      .getByTestId('overview-nodes-link')
      .click()
      .url()
      .should('include', '/nodes')
      .getByTestId('workspace-tab')
      .within(() => {
        cy.getByText('Overview').click();
      })
      .getByTestId('overview-topics-link')
      .click()
      .url()
      .should('include', '/topics')
      .getByTestId('workspace-tab')
      .within(() => {
        cy.getByText('Overview').click();
      })
      .getByTestId('overview-streamapps-link')
      .click()
      .url()
      .should('include', '/streamapps')
      .getByTestId('workspace-tab')
      .within(() => {
        cy.getByText('Overview').click();
      })
      .getByTestId('overview-plugins-link')
      .click()
      .url()
      .should('include', '/plugins');
  });

  it('should display the overview info', () => {
    cy.addTopic().as('overviewTopic');

    cy.visit(WORKSPACES)
      .wait('@getWorkers')
      .uploadTestStreamAppJar(Cypress.env('WORKER_NAME'))
      .getByTestId(Cypress.env('WORKER_NAME'))
      .click()
      .wait('@getWorker')
      .then(xhr => {
        const { imageName, clientPort, nodeNames, jmxPort } = xhr.response.body;

        // Basic info
        cy.getByText(`Worker Image: ${imageName}`).should('have.length', 1);

        // Nodes
        nodeNames.forEach(node => {
          cy.getByText(`${node}:${clientPort}`)
            .should('have.length', 1)
            .getByTestId(`Worker-${node}:${clientPort}`)
            .trigger('mouseover')
            .getByText(`Jmxport: ${jmxPort}`)
            .should('have.length', 1);
        });

        const connectors = xhr.response.body.connectors.filter(
          connector => !CONNECTOR_FILTERS.includes(connector.className),
        );

        connectors.forEach(connector => {
          const name = connector.className.split('.').pop();

          cy.getByText(name)
            .should('have.length', 1)
            .getByTestId(`${name}-tooltip`)
            .trigger('mouseover')
            .getByText(connector.className)
            .should('have.length', 1);

          const definitions = connector.definitions;
          const keys = ['kind', 'version', 'author', 'class'];

          definitions
            .filter(definition => keys.includes(definition.key))
            .forEach((definition, index) => {
              cy.getByTestId(`${keys[index]}-value`).then($el =>
                cy.wrap($el.text()).should('have.eq', definition.defaultValue),
              );
            });

          cy.getByTestId(`${name}-tooltip`).trigger('mouseout');
        });
      })
      .wait('@getBroker')
      .then(xhr => {
        const clientPort = xhr.response.body.clientPort;
        const bkNodes = xhr.response.body.nodeNames;
        const jmxPort = xhr.response.body.jmxPort;
        const exporterPort = xhr.response.body.exporterPort;
        cy.getByText(`Broker Image: ${xhr.response.body.imageName}`).should(
          'have.length',
          1,
        );
        bkNodes.forEach(node => {
          cy.getByText(`${node}:${clientPort}`)
            .should('have.length', 1)
            .getByTestId(`Broker-${node}:${clientPort}`)
            .trigger('mouseover')
            .getByText(`Jmxport: ${jmxPort}`)
            .should('have.length', 1)
            .getByText(`Exporterport: ${exporterPort}`)
            .should('have.length', 1);
        });
      })
      .wait('@getZookeeper')
      .then(xhr => {
        const clientPort = xhr.response.body.clientPort;
        const ZkNodes = xhr.response.body.nodeNames;
        const peerPort = xhr.response.body.peerPort;
        const electionPort = xhr.response.body.electionPort;
        cy.getByText(`Zookeeper Image: ${xhr.response.body.imageName}`).should(
          'have.length',
          1,
        );
        ZkNodes.forEach(node => {
          cy.getByText(`${node}:${clientPort}`)
            .should('have.length', 1)
            .getByTestId(`Zookeeper-${node}:${clientPort}`)
            .trigger('mouseover')
            .getByText(`Peerport: ${peerPort}`)
            .should('have.length', 1)
            .getByText(`Electionport: ${electionPort}`)
            .should('have.length', 1)
            .getByTestId(`Zookeeper-${node}:${clientPort}`)
            .trigger('mouseout');
        });
      })
      .wait('@getTopics')
      .then(xhr => {
        const topics = xhr.response.body;

        cy.get('@overviewTopic').then(res => {
          const currentBroker = res.brokerClusterName;

          topics
            .filter(topic => topic.brokerClusterName === currentBroker)
            .forEach(topic => {
              const { name, numberOfPartitions, numberOfReplications } = topic;
              cy.getByText(name)
                .should('have.length', 1)
                .getByTestId(`${name}-nop-${numberOfPartitions}`)
                .should('have.length', 1)
                .getByTestId(`${name}-nor-${numberOfReplications}`)
                .should('have.length', 1);
            });
        });
      })
      .wait('@getFiles')
      .then(xhr => {
        cy.log(xhr);
        xhr.response.body.forEach(file => {
          const size = floor(divide(file.size, 1024), 1);
          cy.getByText(file.name)
            .should('have.length', 1)
            .getByText(String(size))
            .should('have.length', 1);
        });
      });
  });
});
