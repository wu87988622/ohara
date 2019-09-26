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
    cy.route('GET', 'api/pipelines').as('getPipelines');
    cy.route('PUT', 'api/workers/*/stop').as('stopWorker');
    cy.route('PUT', 'api/workers/*/start').as('startWorker');
    cy.route('GET', 'api/files*').as('getFiles');
    cy.route('POST', 'api/zookeepers').as('createZookeeper');
    cy.route('POST', 'api/brokers').as('createBroker');
  });

  it('creates a workspace', () => {
    const nodeName = Cypress.env('nodeHost');
    const prefix = Cypress.env('servicePrefix');
    const workerName = generate.serviceName({
      prefix: `${prefix}wk`,
      length: 3,
    });

    cy.visit(WORKSPACES, {
      onBeforeLoad(win) {
        win.servicePrefix = prefix; // Add prefix for generated services
      },
    })
      .findByText('NEW WORKSPACE')
      .click()
      .findByPlaceholderText('cluster00')
      .type(workerName)
      .findByTestId(nodeName)
      .click()
      .uploadJar(
        'input[type=file]',
        'plugin/ohara-it-sink.jar',
        'ohara-it-sink.jar',
        'application/java-archive',
      )
      .wait(500)
      .findByText('ohara-it-sink')
      .click()
      .findByText('ADD')
      .click()
      .wait('@createZookeeper')
      .wait('@createBroker');

    cy.findByText(workerName, { timeout: 40000 }).should('have.length', 1);
  });

  it('adds and removes a topic', () => {
    const topicName = generate.serviceName({ prefix: 'topic' });

    cy.visit(WORKSPACES)
      .wait('@getWorkers')
      .findByTestId(Cypress.env('WORKER_NAME'))
      .click()
      .findByTestId('workspace-tab')
      .within(() => {
        cy.findByText('TOPICS').click();
      })
      .findByText('NEW TOPIC')
      .click()
      .findByPlaceholderText('Kafka Topic')
      .type(topicName)
      .findByTestId('partitions-input')
      .type(1)
      .findByTestId('replications-input')
      .type(1)
      .findByText('ADD')
      .click()
      .wait('@getTopics')
      .findByText(topicName)
      .should('have.length', 1);

    cy.findByTestId(topicName)
      .click({ force: true })
      .findByText('DELETE')
      .click()
      .findByText(`Successfully deleted the topic: ${topicName}`)
      .should('have.length', 1);
  });

  it('adds and removes a streamApp', () => {
    cy.visit(WORKSPACES)
      .wait('@getWorkers')
      .findByTestId(Cypress.env('WORKER_NAME'))
      .click()
      .findByTestId('workspace-tab')
      .within(() => {
        cy.findByText('STREAM JARS').click();
      })
      .wait('@getFiles')
      .uploadJar(
        'input[type=file]',
        'streamApp/ohara-streamapp.jar',
        'ohara-streamapp.jar',
        'application/java-archive',
      )
      .wait(500)
      .findByText('ohara-streamapp.jar')
      .should('have.length', 1);

    cy.findByTestId('ohara-streamapp.jar')
      .click()
      .findByText('DELETE')
      .click()
      .findByText('Successfully deleted the stream app!')
      .should('have.length', 1)
      .wait('@getFiles')
      .get('td')
      .should('have.length', 0);
  });

  it('should warn the user when a duplicate jar name is upload', () => {
    cy.visit(WORKSPACES)
      .wait('@getWorkers')
      .findByTestId(Cypress.env('WORKER_NAME'))
      .click()
      .findByTestId('workspace-tab')
      .within(() => {
        cy.findByText('STREAM JARS').click();
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
      .findByText('The jar name already exists!')
      .should('have.length', 1)
      .queryAllByText('ohara-streamapp.jar')
      .should('have.length', 1);
  });

  it('should link to the correct service page', () => {
    cy.visit(WORKSPACES)
      .wait('@getWorkers')
      .findByTestId(Cypress.env('WORKER_NAME'))
      .click()
      .url()
      .should('include', '/overview')
      .findByTestId('overview-nodes-link')
      .click()
      .url()
      .should('include', '/nodes')
      .findByTestId('workspace-tab')
      .within(() => {
        cy.findByText('OVERVIEW').click();
      })
      .findByTestId('overview-topics-link')
      .click()
      .url()
      .should('include', '/topics')
      .findByTestId('workspace-tab')
      .within(() => {
        cy.findByText('OVERVIEW').click();
      })
      .findByTestId('overview-streamapps-link')
      .click()
      .url()
      .should('include', '/streamapps')
      .findByTestId('workspace-tab')
      .within(() => {
        cy.findByText('OVERVIEW').click();
      })
      .findByTestId('overview-plugins-link')
      .click()
      .url()
      .should('include', '/plugins');
  });

  it.only('should display the overview info', () => {
    cy.addTopic().as('overviewTopic');

    cy.visit(WORKSPACES)
      .wait('@getWorkers')
      .uploadTestStreamAppJar(Cypress.env('WORKER_NAME'))
      .findByTestId(Cypress.env('WORKER_NAME'))
      .click()
      .wait('@getWorker')
      .then(xhr => {
        const {
          imageName,
          clientPort,
          nodeNames,
          jmxPort,
        } = xhr.response.body.settings;

        // Basic info
        cy.findByText(`Worker Image: ${imageName}`).should('have.length', 1);

        // Nodes
        nodeNames.forEach(node => {
          cy.findByText(`${node}:${clientPort}`)
            .should('have.length', 1)
            .findByTestId(`Worker-${node}:${clientPort}`)
            .trigger('mouseover')
            .findByText(`Jmxport: ${jmxPort}`)
            .should('have.length', 1);
        });

        const connectors = xhr.response.body.connectors.filter(
          connector => !CONNECTOR_FILTERS.includes(connector.className),
        );

        connectors.forEach(connector => {
          const name = connector.className.split('.').pop();

          cy.findByText(name)
            .should('have.length', 1)
            .findByTestId(`${name}-tooltip`)
            .trigger('mouseover')
            .findByText(connector.className)
            .should('have.length', 1);

          const definitions = connector.definitions;
          const keys = ['kind', 'version', 'author', 'class'];

          definitions
            .filter(definition => keys.includes(definition.key))
            .forEach((definition, index) => {
              cy.findByTestId(`${keys[index]}-value`).then($el =>
                cy.wrap($el.text()).should('have.eq', definition.defaultValue),
              );
            });

          cy.findByTestId(`${name}-tooltip`).trigger('mouseout');
        });
      })
      .wait('@getBroker')
      .then(xhr => {
<<<<<<< HEAD
        const {
          clientPort,
          nodeNames,
          jmxPort,
          exporterPort,
        } = xhr.response.body.settings;
        cy.getByText(
          `Broker Image: ${xhr.response.body.settings.imageName}`,
        ).should('have.length', 1);
        nodeNames.forEach(node => {
          cy.getByText(`${node}:${clientPort}`)
=======
        const clientPort = xhr.response.body.clientPort;
        const bkNodes = xhr.response.body.nodeNames;
        const jmxPort = xhr.response.body.jmxPort;
        const exporterPort = xhr.response.body.exporterPort;
        cy.findByText(`Broker Image: ${xhr.response.body.imageName}`).should(
          'have.length',
          1,
        );
        bkNodes.forEach(node => {
          cy.findByText(`${node}:${clientPort}`)
>>>>>>> wip
            .should('have.length', 1)
            .findByTestId(`Broker-${node}:${clientPort}`)
            .trigger('mouseover')
            .findByText(`Jmxport: ${jmxPort}`)
            .should('have.length', 1)
            .findByText(`Exporterport: ${exporterPort}`)
            .should('have.length', 1);
        });
      })
      .wait('@getZookeeper')
      .then(xhr => {
<<<<<<< HEAD
        const {
          clientPort,
          nodeNames,
          peerPort,
          electionPort,
        } = xhr.response.body.settings;
        cy.getByText(
          `Zookeeper Image: ${xhr.response.body.settings.imageName}`,
        ).should('have.length', 1);
        nodeNames.forEach(node => {
          cy.getByText(`${node}:${clientPort}`)
=======
        const clientPort = xhr.response.body.clientPort;
        const ZkNodes = xhr.response.body.nodeNames;
        const peerPort = xhr.response.body.peerPort;
        const electionPort = xhr.response.body.electionPort;
        cy.findByText(`Zookeeper Image: ${xhr.response.body.imageName}`).should(
          'have.length',
          1,
        );
        ZkNodes.forEach(node => {
          cy.findByText(`${node}:${clientPort}`)
>>>>>>> wip
            .should('have.length', 1)
            .findByTestId(`Zookeeper-${node}:${clientPort}`)
            .trigger('mouseover')
            .findByText(`Peerport: ${peerPort}`)
            .should('have.length', 1)
            .findByText(`Electionport: ${electionPort}`)
            .should('have.length', 1)
            .findByTestId(`Zookeeper-${node}:${clientPort}`)
            .trigger('mouseout');
        });
      })
      .wait('@getTopics')
      .then(xhr => {
        const topics = xhr.response.body;
        cy.get('@overviewTopic').then(topic => {
          const currentBroker = topic.name;

          topics
            .filter(topic => {
              return topic.settings.brokerClusterKey.name === currentBroker;
            })
            .forEach(topic => {
              const { name, numberOfPartitions, numberOfReplications } = topic;
              cy.findByText(name)
                .should('have.length', 1)
                .findByTestId(`${name}-nop-${numberOfPartitions}`)
                .should('have.length', 1)
                .findByTestId(`${name}-nor-${numberOfReplications}`)
                .should('have.length', 1);
            });
        });
      })
      .wait('@getFiles')
      .then(xhr => {
        xhr.response.body.forEach(file => {
          const size = floor(divide(file.size, 1024), 1);
          cy.findByText(file.name)
            .should('have.length', 1)
            .findByText(String(size))
            .should('have.length', 1);
        });
      });
  });
});

describe('plugin', () => {
  let workerName;
  const jarName = 'ohara-it-sink.jar';
  before(() => {
    const prefix = Cypress.env('servicePrefix');
    workerName = generate.serviceName({
      prefix: `${prefix}wk`,
      length: 5,
    });
    cy.deleteTestPlugin({ jarName, workerName });
    cy.removeWorkers();
    cy.addWorker({ jarName, workerName });
  });

  beforeEach(() => {
    cy.server();
    cy.route('GET', 'api/workers').as('getWorkers');
    cy.route('GET', 'api/workers/*').as('getWorker');
    cy.route('GET', 'api/brokers/*').as('getBroker');
    cy.route('GET', 'api/zookeepers/*').as('getZookeeper');
    cy.route('GET', 'api/topics/*').as('getTopics');
    cy.route('GET', 'api/pipelines').as('getPipelines');
    cy.route('GET', 'api/connectors/*').as('getConnectors');
    cy.route('PUT', 'api/workers/*/stop').as('stopWorker');
    cy.route('PUT', 'api/workers/*/start').as('startWorker');
    cy.route('PUT', 'api/workers/*').as('putWorker');
    cy.route('PUT', 'api/connectors/*/start?*').as('startConnector');
    cy.route('PUT', 'api/connectors/*/stop?*').as('stopConnector');
    cy.route('GET', 'api/files').as('getFiles');
  });

  it('should update a workespace', () => {
    cy.visit(WORKSPACES)
      .wait('@getWorkers')
      .findByTestId(workerName)
      .click()
      .findByTestId('workspace-tab')
      .within(() => {
        cy.findByText('PLUGINS').click();
      })
      .wait('@getFiles')
      .uploadJar(
        'input[type=file]',
        'plugin/ohara-it-source.jar',
        'ohara-it-source.jar',
        'application/java-archive',
      )
      .wait('@getFiles')
      .findByText('Plugin successfully uploaded!')
      .should('have.length', 1)
      .queryAllByText('ohara-it-source.jar')
      .should('have.length', 1)
      .findByText(
        'You’ve made some changes to the plugins: 1 added. Please restart for these settings to take effect!!',
      )
      .should('have.length', 1)
      .findByText('RESTART')
      .click()
      .findByTestId('confirm-button-RESTART')
      .click()
      .wait('@getPipelines', { timeout: 60000 })
      .wait('@stopWorker', { timeout: 60000 })
      .wait('@startWorker', { timeout: 60000 })
      .wait('@getFiles', { timeout: 60000 })
      .findByTestId('plugins-loaded', { timeout: 60000 })
      .should('have.length', 1);
  });

  it('should faild to update a workespace', () => {
    const pipelineName = generate.serviceName({ prefix: 'pipeline' });
    const perfName = generate.serviceName({ prefix: 'perf' });
    const topicName = generate.serviceName({ prefix: 'topic' });
    const dumbsinkName = generate.serviceName({ prefix: 'dumbsink' });
    cy.addPipeline({
      name: pipelineName,
      group: `${workerName}${pipelineName}`,
      tags: {
        workerClusterName: workerName,
      },
    });
    cy.addTopic(topicName, workerName);
    cy.addConnector({
      'connector.class': 'com.island.ohara.connector.perf.PerfSource',
      group: `${workerName}${pipelineName}`,
      name: perfName,
      workerClusterKey: { name: workerName },
    });
    cy.putConnector({
      url: `/${perfName}?group=${workerName}${pipelineName}`,
      param: {
        topicKeys: [{ group: `${workerName}`, name: topicName }],
      },
    });
    cy.addConnector({
      'connector.class': 'com.island.ohara.it.connector.DumbSinkConnector',
      group: `${workerName}${pipelineName}`,
      name: dumbsinkName,
      workerClusterKey: { name: workerName },
    });
    cy.putConnector({
      url: `/${dumbsinkName}?group=${workerName}${pipelineName}`,
      param: {
        topicKeys: [{ group: `${workerName}`, name: topicName }],
      },
    });
    cy.putPipeline({
      url: `/${pipelineName}?group=${workerName}${pipelineName}`,
      param: {
        flows: [
          {
            from: {
              group: `${workerName}${pipelineName}`,
              name: perfName,
            },
            to: [
              {
                group: `${workerName}${pipelineName}`,
                name: topicName,
              },
            ],
          },
          {
            from: {
              group: `${workerName}${pipelineName}`,
              name: dumbsinkName,
            },
            to: [],
          },
          {
            from: {
              group: `${workerName}`,
              name: topicName,
            },
            to: [{ group: `${workerName}`, name: dumbsinkName }],
          },
        ],
      },
    });
    cy.putConnector({
      url: `/${perfName}/start?group=${workerName}${pipelineName}`,
    });
    cy.putConnector({
      url: `/${dumbsinkName}/start?group=${workerName}${pipelineName}`,
    });
    cy.visit(WORKSPACES)
      .wait('@getWorkers')
      .findByTestId(workerName)
      .click()
      .findByTestId('workspace-tab')
      .within(() => {
        cy.findByText('PLUGINS').click();
      })
      .wait('@getFiles')
      .findByTestId('ohara-it-sink.jar')
      .click()
      .findByText('RESTART')
      .click()
      .findByTestId('confirm-button-RESTART')
      .click()
      .wait('@getPipelines', { timeout: 60000 })
      .wait('@stopConnector', { timeout: 60000 })
      .wait('@stopConnector', { timeout: 60000 })
      .wait('@getConnectors', { timeout: 60000 })
      .wait('@stopWorker', { timeout: 60000 })
      .wait('@startWorker', { timeout: 60000 })
      .wait('@startConnector', { timeout: 60000 })
      .wait('@startConnector', { timeout: 60000 })
      .wait('@stopWorker', { timeout: 60000 })
      .wait('@putWorker', { timeout: 60000 })
      .wait('@startWorker', { timeout: 60000 })
      .wait('@startConnector', { timeout: 60000 })
      .wait('@startConnector', { timeout: 60000 })
      .wait('@getFiles', { timeout: 60000 })
      .findByTestId('ohara-it-sink.jar', { timeout: 60000 })
      .should('have.length', 1);
  });
});
