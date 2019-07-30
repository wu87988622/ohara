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
import { divide, floor } from 'lodash';
import * as utils from '../utils';

describe('WorkspacesPage', () => {
  before(() => {
    cy.deleteAllWorkers();
    cy.createWorker();
  });

  beforeEach(() => {
    cy.server();
    cy.route('GET', 'api/workers').as('getWorkers');
    cy.route('GET', 'api/workers/*').as('getWorker');
    cy.route('GET', 'api/brokers/*').as('getBroker');
    cy.route('GET', 'api/zookeepers/*').as('getZookeeper');
    cy.route('GET', 'api/topics').as('getTopics');
    cy.route('GET', 'api/files?*').as('getFiles');
  });

  it('creates a new connect worker cluster', () => {
    const nodeName = Cypress.env('nodeHost');
    const clusterName = utils.makeRandomStr();

    cy.registerWorker(clusterName);

    cy.visit(WORKSPACES)
      .getByText('New workspace')
      .click()
      .getByPlaceholderText('cluster00')
      .type(clusterName)
      .getByTestId(nodeName)
      .click();

    cy.uploadJar(
      'input[type=file]',
      'plugin/ohara-it-sink.jar',
      'ohara-it-sink.jar',
      'application/java-archive',
    ).wait(500);

    cy.getByText('ohara-it-sink').should('have.length', 1);
    cy.getByText('ohara-it-sink').click();
    cy.getByText('Add').click();

    cy.getByText(clusterName).should('have.length', 1);
  });

  it('adds a new topic', () => {
    const topicName = utils.makeRandomStr();

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
      .getByPlaceholderText('1')
      .type(1)
      .getByPlaceholderText('3')
      .type(1)
      .getByText('Save')
      .click()
      .wait('@getTopics')
      .getByText(topicName)
      .should('have.length', 1);
  });

  it('deletes a topic', () => {
    cy.createTopic().as('newTopic');

    cy.visit(WORKSPACES)
      .wait('@getWorkers')
      .getByTestId(Cypress.env('WORKER_NAME'))
      .click()
      .getByTestId('workspace-tab')
      .within(() => {
        cy.getByText('Topics').click();
      })
      .wait('@getTopics')
      .get('@newTopic')
      .then(topic => {
        cy.getByTestId(topic.name)
          .click({ force: true })
          .getByText('Delete')
          .click()
          .getByText(`Successfully deleted the topic: ${topic.name}`)
          .should('have.length', 1);
      });
  });

  it('adds and removes a new streamApp', () => {
    cy.visit(WORKSPACES)
      .wait('@getWorkers')
      .getByTestId(Cypress.env('WORKER_NAME'))
      .click()
      .getByTestId('workspace-tab')
      .within(() => {
        cy.getByText('Stream apps').click();
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
      .getByText('Delete')
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
        cy.getByText('Stream apps').click();
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
      .should('include', '/streamapps');
  });

  it('should display the overview info', () => {
    cy.createTopic().as('overviewTopic');

    cy.visit(WORKSPACES)
      .wait('@getWorkers')
      .uploadTestStreamAppJar(Cypress.env('WORKER_NAME'))
      .getByTestId(Cypress.env('WORKER_NAME'))
      .click()
      .wait('@getWorker')
      .then(xhr => {
        cy.getByText(`Image: ${xhr.response.body.imageName}`).should(
          'have.length',
          1,
        );
        const clientPort = xhr.response.body.clientPort;
        const wkNodes = xhr.response.body.nodeNames;
        const jmxPort = xhr.response.body.jmxPort;
        wkNodes.forEach(node => {
          cy.getByText(`${node}:${clientPort}`)
            .should('have.length', 1)
            .getByTestId(`Worker-${node}:${clientPort}`)
            .trigger('mouseover')
            .getByText(`Jmxport: ${jmxPort}`)
            .should('have.length', 1);
        });

        const connectors = xhr.response.body.connectors;
        connectors.forEach(connector => {
          const name = connector.className.split('.').pop();
          if (name === 'PerfSource') return;

          cy.getByText(name)
            .should('have.length', 1)
            .getByTestId(`${name}-tooltip`)
            .trigger('mouseover')
            .getByText(connector.className)
            .should('have.length', 1);

          const definitions = connector.definitions;
          const keys = ['kind', 'version', 'author'];

          definitions.forEach(definition => {
            if (keys.includes(definition.key)) {
              cy.getByText(definition.defaultValue).should('have.length', 1);
            }
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
        xhr.response.body.forEach(streamapp => {
          const size = floor(divide(streamapp.size, 1024), 2);
          cy.getByText(streamapp.name)
            .should('have.length', 1)
            .getByText(String(size))
            .should('have.length', 1);
        });
      });
  });
});
