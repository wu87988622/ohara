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

import { makeRandomPort } from '../utils';

let brokerClusterName = '';
let jar = {};
let jarGroup = '';
let pipelineName = '';
let topicName = '';
let propertyName = '';
let fakeWorkerName = '';
const nodeName = `node${makeRandomPort()}`;
const zookeeperName = `zk${makeRandomPort()}`;
const wkName = `wk${makeRandomPort()}`;
const connectorName = `source${makeRandomPort()}`;

describe('Nodes', () => {
  it('createNode', () => {
    const data = {
      name: nodeName,
      port: 22,
      user: 'ohara',
      password: '123',
    };
    cy.createNode(data).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { services, name, password, port, user, lastModified } = result;
      const [zookeeper, broker, worker] = services;

      expect(isSuccess).to.eq(true);

      expect(services).to.be.a('array');
      expect(name).to.be.a('string');
      expect(password).to.be.a('string');
      expect(port).to.be.a('number');
      expect(user).to.be.a('string');
      expect(lastModified).to.be.a('number');

      expect(zookeeper.name).to.eq('zookeeper');
      expect(broker.name).to.eq('broker');
      expect(worker.name).to.eq('connect-worker');

      expect(zookeeper.name).to.be.a('string');
      expect(zookeeper.clusterNames).to.be.a('array');
      expect(broker.name).to.be.a('string');
      expect(broker.clusterNames).to.be.a('array');
      expect(worker.name).to.be.a('string');
      expect(worker.clusterNames).to.be.a('array');
    });
  });
  it('updateNode', () => {
    const data = {
      name: nodeName,
      port: 23,
      user: 'ohara123',
      password: '1234',
    };

    cy.updateNode(data).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { services, name, password, port, user, lastModified } = result;
      const [zookeeper, broker, worker] = services;

      expect(isSuccess).to.eq(true);

      expect(services).to.be.a('array');
      expect(name).to.be.a('string');
      expect(port).to.eq(23);
      expect(user).to.eq('ohara123');
      expect(password).to.eq('1234');
      expect(lastModified).to.be.a('number');

      expect(zookeeper.name).to.eq('zookeeper');
      expect(broker.name).to.eq('broker');
      expect(worker.name).to.eq('connect-worker');

      expect(zookeeper.name).to.be.a('string');
      expect(zookeeper.clusterNames).to.be.a('array');
      expect(broker.name).to.be.a('string');
      expect(broker.clusterNames).to.be.a('array');
      expect(worker.name).to.be.a('string');
      expect(worker.clusterNames).to.be.a('array');
    });
  });

  it('fetchNodes', () => {
    cy.fetchNodes().then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { services, name, password, port, user, lastModified } = result[0];
      const [zookeeper, broker, worker] = services;

      expect(isSuccess).to.eq(true);

      expect(result[0]).to.be.a('object');
      expect(services).to.be.a('array');
      expect(name).to.be.a('string');
      expect(password).to.be.a('string');
      expect(port).to.be.a('number');
      expect(user).to.be.a('string');
      expect(lastModified).to.be.a('number');

      expect(zookeeper.name).to.eq('zookeeper');
      expect(broker.name).to.eq('broker');
      expect(worker.name).to.eq('connect-worker');

      expect(zookeeper.name).to.be.a('string');
      expect(zookeeper.clusterNames).to.be.a('array');
      expect(broker.name).to.be.a('string');
      expect(broker.clusterNames).to.be.a('array');
      expect(worker.name).to.be.a('string');
      expect(worker.clusterNames).to.be.a('array');
    });
  });
});

describe('Zookeepers', () => {
  it('createZookeeper', () => {
    const params = {
      name: zookeeperName,
      clientPort: makeRandomPort(),
      peerPort: makeRandomPort(),
      electionPort: makeRandomPort(),
      nodeNames: [nodeName],
    };
    cy.createZookeeper(params).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { name, clientPort, electionPort, peerPort, nodeNames } = result;
      expect(isSuccess).to.eq(true);

      expect(name).to.be.a('string');
      expect(nodeNames).to.be.a('array');
      expect(clientPort).to.be.a('number');
      expect(electionPort).to.be.a('number');
      expect(peerPort).to.be.a('number');
    });
  });

  it('fetchZookeepers', () => {
    cy.fetchZookeepers().then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { name, clientPort, electionPort, peerPort, nodeNames } = result[0];

      expect(isSuccess).to.eq(true);

      expect(name).to.be.a('string');
      expect(nodeNames).to.be.a('array');
      expect(clientPort).to.be.a('number');
      expect(electionPort).to.be.a('number');
      expect(peerPort).to.be.a('number');
    });
  });

  it('fetchZookeeper', () => {
    cy.fetchZookeeper(zookeeperName).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { name, clientPort, electionPort, peerPort, nodeNames } = result;

      expect(isSuccess).to.eq(true);

      expect(name).to.be.a('string');
      expect(nodeNames).to.be.a('array');
      expect(clientPort).to.be.a('number');
      expect(electionPort).to.be.a('number');
      expect(peerPort).to.be.a('number');
    });
  });

  it('startZookeeper', () => {
    cy.startZookeeper(zookeeperName).then(res => {
      expect(res.data.isSuccess).to.eq(true);
    });

    cy.fetchZookeeper(zookeeperName).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { state } = result;

      expect(isSuccess).to.eq(true);

      expect(state).to.be.eq('RUNNING');
    });
  });

  it('stopZookeeper', () => {
    cy.stopZookeeper(zookeeperName).then(res => {
      expect(res.data.isSuccess).to.eq(true);
    });

    cy.fetchZookeeper(zookeeperName).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { state } = result;

      expect(isSuccess).to.eq(true);

      expect(state).to.be.eq(undefined);
    });
  });

  it('deleteZookeeper', () => {
    const newZKName = `zk${makeRandomPort()}`;
    const params = {
      name: newZKName,
      clientPort: makeRandomPort(),
      peerPort: makeRandomPort(),
      electionPort: makeRandomPort(),
      nodeNames: [nodeName],
    };
    cy.createZookeeper(params).then(res => {
      const { name } = res.data.result;

      cy.deleteZookeeper(name).then(deleteRes => {
        const {
          data: { isSuccess },
        } = deleteRes;
        expect(isSuccess).to.eq(true);
      });
    });
  });
});

describe('Brokers', () => {
  it('fetchBrokers', () => {
    cy.fetchBrokers().then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { name, clientPort, jmxPort, exporterPort, nodeNames } = result[0];

      brokerClusterName = name;
      expect(isSuccess).to.eq(true);

      expect(clientPort).to.be.a('number');
      expect(name).to.be.a('string');
      expect(nodeNames).to.be.a('array');
      expect(jmxPort).to.be.a('number');
      expect(exporterPort).to.be.a('number');
    });
  });
});

describe('Jars', () => {
  const testJarName = 'ohara-it-source.jar';

  it('createJar', () => {
    cy.createJar(testJarName).then(res => {
      const { data } = res;
      jar = {
        name: data.result.name,
        group: data.result.group,
      };
      jarGroup = data.result.group;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys('name', 'group');
      expect(data.result.name).to.be.a('string');
      expect(data.result.group).to.be.a('string');
    });
  });

  it('fetchJars', () => {
    cy.fetchJars(jarGroup).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.be.a('array');
      expect(data.result[0]).to.include.keys('name', 'group');
      expect(data.result[0].name).to.be.a('string');
      expect(data.result[0].group).to.be.a('string');
    });
  });
});

describe('Workers', () => {
  it('createWorker', () => {
    const data = {
      name: wkName,
      jmxPort: makeRandomPort(),
      brokerClusterName: brokerClusterName,
      clientPort: makeRandomPort(),
      nodeNames: [nodeName],
      plugins: [jar],
    };

    cy.testCreateWorker(data).then(res => {
      const {
        data: { isSuccess, result },
      } = res;

      expect(isSuccess).to.eq(true);

      expect(result.name).to.be.a('string');
      expect(result.clientPort).to.be.a('number');
      expect(result.nodeNames).to.be.a('array');
      expect(result.connectors).to.be.a('array');
      expect(result.jarInfos).to.be.a('array');
      expect(result.configTopicName).to.be.a('string');
      expect(result.offsetTopicName).to.be.a('string');
      expect(result.statusTopicName).to.be.a('string');
    });
  });

  it('fetchWorker', () => {
    cy.fetchWorker(wkName).then(res => {
      const {
        data: { isSuccess, result },
      } = res;

      fakeWorkerName = result.name;

      expect(isSuccess).to.eq(true);
      expect(result.name).to.be.a('string');
      expect(result.clientPort).to.be.a('number');
      expect(result.nodeNames).to.be.a('array');
      expect(result.connectors).to.be.a('array');
      expect(result.jarInfos).to.be.a('array');
    });
  });
});

describe('Topics', () => {
  const tpName = `tp${makeRandomPort()}`;

  it('CreateTopic', () => {
    const data = {
      name: tpName,
      brokerClusterName: brokerClusterName,
      numberOfPartitions: 1,
      numberOfReplications: 1,
    };
    cy.testCreateTopic(data).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const {
        name,
        numberOfPartitions,
        numberOfReplications,
        metrics,
      } = result;
      topicName = name;

      expect(isSuccess).to.eq(true);
      expect(name).to.be.a('string');
      expect(numberOfPartitions).to.be.a('number');
      expect(numberOfReplications).to.be.a('number');
      expect(metrics).to.be.a('object');
      expect(metrics.meters).to.be.a('array');
    });
  });

  it('fetchTopic', () => {
    cy.fetchTopic(tpName).then(res => {
      const {
        data: { isSuccess, result },
      } = res;

      const {
        name,
        numberOfPartitions,
        numberOfReplications,
        metrics,
      } = result;

      expect(isSuccess).to.eq(true);
      expect(name).to.be.a('string');
      expect(numberOfPartitions).to.be.a('number');
      expect(numberOfReplications).to.be.a('number');
      expect(metrics).to.be.a('object');
      expect(metrics.meters).to.be.a('array');
    });
  });
});

describe.skip('Pipelines', () => {
  it('createPipeline', () => {
    const params = {
      name: 'fakePipeline',
      rules: {},
      workerClusterName: wkName,
    };

    cy.testCreatePipeline(params).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { name, workerClusterName, objects } = result;

      pipelineName = name;
      expect(isSuccess).to.eq(true);
      expect(name).to.be.a('string');
      expect(workerClusterName).to.be.a('string');
      expect(objects).to.be.a('array');
    });
  });

  it('fetchPipeline', () => {
    cy.fetchPipeline(pipelineName).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { name, workerClusterName, objects } = result;

      expect(isSuccess).to.eq(true);
      expect(name).to.be.a('string');
      expect(workerClusterName).to.be.a('string');
      expect(objects).to.be.a('array');
    });
  });

  it('fetchPipelines', () => {
    cy.fetchPipelines().then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { name, workerClusterName, objects } = result[0];

      expect(isSuccess).to.eq(true);
      expect(name).to.be.a('string');
      expect(workerClusterName).to.be.a('string');
      expect(objects).to.be.a('array');
    });
  });

  it('updatePipeline', () => {
    const data = {
      params: {
        name: pipelineName,
        rules: {
          [topicName]: [],
        },
        workerClusterName: wkName,
      },
    };

    cy.updatePipeline(data).then(res => {
      const {
        data: { isSuccess, result },
      } = res;

      expect(isSuccess).to.eq(true);
      expect(result.name).to.be.a('string');
      expect(result.workerClusterName).to.be.a('string');
      expect(result.objects).to.be.a('array');
      expect(result.rules).to.be.a('object');
      expect(result.objects[0].id).to.eq(topicName);
      expect(result.objects[0].kind).to.eq('topic');
      expect(result.objects[0].name).to.eq(topicName);
    });
  });

  it('deletePipeline', () => {
    cy.testDeletePipeline(pipelineName).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
    });
  });
});

describe('Connectors', () => {
  it('createConnector', () => {
    const data = {
      className: 'com.island.ohara.connector.ftp.FtpSource',
      configs: {},
      'connector.name': connectorName,
      name: connectorName,
      numberOfTasks: 1,
      schema: [],
      topics: [],
      workerClusterName: fakeWorkerName,
    };

    cy.createConnector(data).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { settings } = result;

      expect(isSuccess).to.eq(true);

      expect(settings).to.be.a('object');
      expect(settings.className).to.be.a('string');
      expect(settings['connector.name']).to.be.a('string');
      expect(settings['tasks.max']).to.be.a('number');
      expect(settings.name).to.be.a('string');
      expect(settings.workerClusterName).to.be.a('string');
    });
  });

  it('fetchConnector', () => {
    cy.fetchConnector(connectorName).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { settings } = result;

      expect(isSuccess).to.eq(true);
      expect(result).to.include.keys('settings');
      expect(settings).to.be.a('object');
      expect(settings['className']).to.be.a('string');
      expect(settings['connector.name']).to.be.a('string');
      expect(settings['tasks.max']).to.be.a('number');
      expect(settings.name).to.be.a('string');
      expect(settings.workerClusterName).to.be.a('string');
    });
  });

  it('updateConnector', () => {
    const params = {
      name: connectorName,
      params: {
        name: connectorName,
        author: 'root',
        columns: [
          { dataType: 'STRING', name: 'test', newName: 'test', order: 1 },
        ],
        className: 'com.island.ohara.connector.ftp.FtpSource',
        'connector.name': 'Untitled source',
        'ftp.completed.folder': 'test',
        'ftp.encode': 'UTF-8',
        'ftp.error.folder': 'test',
        'ftp.hostname': 'test',
        'ftp.input.folder': 'test',
        'ftp.port': 20,
        'ftp.user.name': 'test',
        'ftp.user.password': 'test',
        kind: 'source',
        revision: '1e7da9544e6aa7ad2f9f2792ed8daf5380783727',
        'tasks.max': 1,
        topics: [topicName],
        version: '0.7.0-SNAPSHOT',
        workerClusterName: fakeWorkerName,
      },
    };

    cy.updateConnector(params).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { settings } = result;

      expect(isSuccess).to.eq(true);

      expect(settings).to.be.a('object');
      expect(settings.author).to.be.a('string');
      expect(settings.columns).to.be.a('array');
      expect(settings.className).to.be.a('string');
      expect(settings.name).to.be.a('string');
      expect(settings['connector.name']).to.be.a('string');
      expect(settings['ftp.completed.folder']).to.be.a('string');
      expect(settings['ftp.encode']).to.be.a('string');
      expect(settings['ftp.error.folder']).to.be.a('string');
      expect(settings['ftp.hostname']).to.be.a('string');
      expect(settings['ftp.input.folder']).to.be.a('string');
      expect(settings['ftp.port']).to.be.a('number');
      expect(settings['ftp.user.name']).to.be.a('string');
      expect(settings['ftp.user.password']).to.be.a('string');
      expect(settings.kind).to.be.a('string');
      expect(settings.revision).to.be.a('string');
      expect(settings['tasks.max']).to.be.a('number');
      expect(settings.topics).to.be.a('array');
      expect(settings.version).to.be.a('string');
      expect(settings.workerClusterName).to.be.a('string');
    });
  });

  it('startConnector', () => {
    cy.startConnector(connectorName).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys('settings', 'state');
      expect(data.result.state).to.be.a('string');
    });
  });

  it('stopConnector', () => {
    cy.stopConnector(connectorName).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys('settings');
    });
  });

  it('deleteConnector', () => {
    cy.deleteConnector(connectorName).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
    });
  });
});

describe('Streamapps', () => {
  it('createProperty', () => {
    const params = {
      jar: jar,
      name: 'streamapp',
    };

    cy.createProperty(params).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { instances, name, from, to, jar } = result;

      propertyName = name;

      expect(isSuccess).to.eq(true);

      expect(name).to.be.a('string');
      expect(instances).to.be.a('number');
      expect(from).to.be.a('array');
      expect(to).to.be.a('array');
      expect(jar).to.be.a('object');
      expect(jar).to.include.keys('name', 'group');
    });
  });

  it('fetchProperty', () => {
    cy.fetchProperty(propertyName).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { instances, name, from, to, jar } = result;

      expect(isSuccess).to.eq(true);

      expect(instances).to.be.a('number');
      expect(name).to.be.a('string');
      expect(from).to.be.a('array');
      expect(to).to.be.a('array');
      expect(jar).to.be.a('object');
      expect(jar).to.include.keys('name', 'group');
    });
  });

  it('updateProperty', () => {
    const params = {
      name: propertyName,
      from: [topicName],
      instances: 1,
    };

    cy.updateProperty(params).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { instances, name, from, to, jar } = result;

      expect(isSuccess).to.eq(true);

      expect(instances).to.be.a('number');
      expect(name).to.be.a('string');
      expect(from).to.be.a('array');
      expect(to).to.be.a('array');
      expect(jar).to.be.a('object');
      expect(jar).to.include.keys('name', 'group');
      expect(from[0]).to.eq(topicName);
    });
  });

  it('stopStreamApp', () => {
    cy.stopStreamApp(propertyName).then(res => {
      expect(res.data.isSuccess).to.eq(true);
    });
  });

  it('deleteProperty', () => {
    cy.deleteProperty(propertyName).then(res => {
      expect(res.data.isSuccess).to.eq(true);
    });
  });
});

describe('Validates', () => {
  it.skip('validateConnector', () => {
    const params = {
      author: 'root',
      columns: [
        { dataType: 'STRING', name: 'test', newName: 'test', order: 1 },
      ],
      name: 'source',
      'connector.class': 'com.island.ohara.connector.ftp.FtpSource',
      'connector.name': 'source',
      'ftp.completed.folder': 'test',
      'ftp.encode': 'UTF-8',
      'ftp.error.folder': 'test',
      'ftp.hostname': 'test',
      'ftp.input.folder': 'test',
      'ftp.port': 20,
      'ftp.user.name': 'test',
      'ftp.user.password': 'test',
      kind: 'source',
      revision: '1e7da9544e6aa7ad2f9f2792ed8daf5380783727',
      'tasks.max': 1,
      topics: [topicName],
      version: '0.7.0-SNAPSHOT',
      workerClusterName: fakeWorkerName,
    };
    cy.validateConnector(params).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result.errorCount).to.eq(0);
      expect(data.result).to.include.keys('errorCount', 'settings');
      expect(data.result.settings).to.be.a('array');
    });
  });
});
