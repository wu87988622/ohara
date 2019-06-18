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
let jarId = '';
let pipelineId = '';
let pipelineName = '';
let topicId = '';
let streamAppId = '';
let propertyId = '';
let fakeWk = '';
const nodeName = `node${makeRandomPort()}`;
const wkName = `wk${makeRandomPort()}`;
const connectorName = `source${makeRandomPort()}`;

describe('Node Api test', () => {
  it('createNode', () => {
    const data = {
      name: nodeName,
      port: 22,
      user: 'ohara',
      password: '123',
    };
    cy.createNode(data).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys(
        'name',
        'password',
        'port',
        'user',
        'services',
      );
      expect(data.result.services).to.be.a('array');
      expect(data.result.services[0]).to.include.keys('name', 'clusterNames');
      expect(data.result.services[1]).to.include.keys('name', 'clusterNames');
      expect(data.result.services[2]).to.include.keys('name', 'clusterNames');
      expect(data.result.services[0].name).to.eq('zookeeper');
      expect(data.result.services[1].name).to.eq('broker');
      expect(data.result.services[2].name).to.eq('connect-worker');
      expect(data.result.name).to.be.a('string');
      expect(data.result.password).to.be.a('string');
      expect(data.result.port).to.be.a('number');
      expect(data.result.user).to.be.a('string');
      expect(data.result.services[0].name).to.be.a('string');
      expect(data.result.services[0].clusterNames).to.be.a('array');
      expect(data.result.services[1].name).to.be.a('string');
      expect(data.result.services[1].clusterNames).to.be.a('array');
      expect(data.result.services[2].name).to.be.a('string');
      expect(data.result.services[2].clusterNames).to.be.a('array');
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
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys(
        'name',
        'password',
        'port',
        'user',
        'services',
      );
      expect(data.result.port).to.eq(23);
      expect(data.result.user).to.eq('ohara123');
      expect(data.result.password).to.eq('1234');
      expect(data.result.services).to.be.a('array');
      expect(data.result.services[0]).to.include.keys('name', 'clusterNames');
      expect(data.result.services[1]).to.include.keys('name', 'clusterNames');
      expect(data.result.services[2]).to.include.keys('name', 'clusterNames');
      expect(data.result.services[0].name).to.eq('zookeeper');
      expect(data.result.services[1].name).to.eq('broker');
      expect(data.result.services[2].name).to.eq('connect-worker');
      expect(data.result.name).to.be.a('string');
      expect(data.result.password).to.be.a('string');
      expect(data.result.port).to.be.a('number');
      expect(data.result.user).to.be.a('string');
      expect(data.result.services[0].name).to.be.a('string');
      expect(data.result.services[0].clusterNames).to.be.a('array');
      expect(data.result.services[1].name).to.be.a('string');
      expect(data.result.services[1].clusterNames).to.be.a('array');
      expect(data.result.services[2].name).to.be.a('string');
      expect(data.result.services[2].clusterNames).to.be.a('array');
    });
  });
  it('fetchNodes', () => {
    cy.fetchNodes().then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.be.a('array');
      expect(data.result[0]).to.include.keys(
        'name',
        'password',
        'port',
        'user',
        'services',
      );
      expect(data.result[0].services).to.be.a('array');
      expect(data.result[0].services[0]).to.include.keys(
        'name',
        'clusterNames',
      );
      expect(data.result[0].services[1]).to.include.keys(
        'name',
        'clusterNames',
      );
      expect(data.result[0].services[2]).to.include.keys(
        'name',
        'clusterNames',
      );
      expect(data.result[0].services[0].name).to.eq('zookeeper');
      expect(data.result[0].services[1].name).to.eq('broker');
      expect(data.result[0].services[2].name).to.eq('connect-worker');
      expect(data.result[0].name).to.be.a('string');
      expect(data.result[0].password).to.be.a('string');
      expect(data.result[0].port).to.be.a('number');
      expect(data.result[0].user).to.be.a('string');
      expect(data.result[0].services[0].name).to.be.a('string');
      expect(data.result[0].services[0].clusterNames).to.be.a('array');
      expect(data.result[0].services[1].name).to.be.a('string');
      expect(data.result[0].services[1].clusterNames).to.be.a('array');
      expect(data.result[0].services[2].name).to.be.a('string');
      expect(data.result[0].services[2].clusterNames).to.be.a('array');
    });
  });
});

describe('Zookeeper Api test', () => {
  it('fetchZookeepers', () => {
    cy.fetchZookeepers().then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result[0]).to.include.keys('clientPort', 'name', 'nodeNames');
      expect(data.result[0].clientPort).to.be.a('number');
      expect(data.result[0].name).to.be.a('string');
      expect(data.result[0].nodeNames).to.be.a('array');
    });
  });
});

describe('Broker Api test', () => {
  it('fetchBrokers', () => {
    cy.fetchBrokers().then(res => {
      const { data } = res;
      brokerClusterName = data.result[0].name;
      expect(data.isSuccess).to.eq(true);
      expect(data.result[0]).to.include.keys('clientPort', 'name', 'nodeNames');
      expect(data.result[0].clientPort).to.be.a('number');
      expect(data.result[0].name).to.be.a('string');
      expect(data.result[0].nodeNames).to.be.a('array');
    });
  });
});

describe('Jar Api test', () => {
  const testJarName = 'ohara-it-source.jar';
  it('createJar', () => {
    cy.createJar(testJarName).then(res => {
      const { data } = res;
      jarId = data.result.id;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys('name', 'id');
      expect(data.result.name).to.be.a('string');
      expect(data.result.id).to.be.a('string');
    });
  });
  it('fetchJars', () => {
    cy.fetchJars().then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.be.a('array');
      expect(data.result[0]).to.include.keys('name', 'id');
      expect(data.result[0].name).to.be.a('string');
      expect(data.result[0].id).to.be.a('string');
    });
  });
});

describe('Worker Api test', () => {
  it('createWorker', () => {
    const data = {
      name: wkName,
      jmxPort: makeRandomPort(),
      brokerClusterName: brokerClusterName,
      clientPort: makeRandomPort(),
      nodeNames: [nodeName],
      plugins: [jarId],
    };
    cy.testCreateWorker(data).then(res => {
      cy.log(res);
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys(
        'name',
        'clientPort',
        'nodeNames',
        'sources',
        'sinks',
        'jarNames',
        'configTopicName',
        'offsetTopicName',
        'statusTopicName',
      );
      expect(data.result.name).to.be.a('string');
      expect(data.result.clientPort).to.be.a('number');
      expect(data.result.nodeNames).to.be.a('array');
      expect(data.result.sources).to.be.a('array');
      expect(data.result.sinks).to.be.a('array');
      expect(data.result.jarNames).to.be.a('array');
      expect(data.result.configTopicName).to.be.a('string');
      expect(data.result.offsetTopicName).to.be.a('string');
      expect(data.result.statusTopicName).to.be.a('string');
    });
  });
  it('fetchWorker', () => {
    cy.fetchWorker(wkName).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result.name).to.be.a('string');
      expect(data.result.clientPort).to.be.a('number');
      expect(data.result.nodeNames).to.be.a('array');
      expect(data.result.sources).to.be.a('array');
      expect(data.result.sinks).to.be.a('array');
      expect(data.result.jarNames).to.be.a('array');
    });
  });
  it('fetchWorkers', () => {
    cy.fetchWorkers().then(res => {
      const { data } = res;
      fakeWk = data.result[0].name;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.be.a('array');
      expect(data.result[0]).to.include.keys(
        'name',
        'nodeNames',
        'configTopicName',
        'offsetTopicName',
        'statusTopicName',
      );
      expect(data.result[0].name).to.be.a('string');
      expect(data.result[0].nodeNames).to.be.a('array');
      expect(data.result[0].configTopicName).to.be.a('string');
      expect(data.result[0].offsetTopicName).to.be.a('string');
      expect(data.result[0].statusTopicName).to.be.a('string');
    });
  });
});

describe('Topic Api test', () => {
  const tpName = `tp${makeRandomPort()}`;
  it('CreateTopic', () => {
    const data = {
      name: tpName,
      numberOfPartitions: 1,
      brokerClusterName: brokerClusterName,
      numberOfReplications: 1,
    };
    cy.testCreateTopic(data).then(res => {
      const { data } = res;
      topicId = data.result.id;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys(
        'id',
        'numberOfPartitions',
        'numberOfReplications',
      );
      expect(data.result.id).to.be.a('string');
      expect(data.result.numberOfPartitions).to.be.a('number');
      expect(data.result.numberOfReplications).to.be.a('number');
    });
  });
  it('fetchTopic', () => {
    cy.fetchTopic(tpName).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys(
        'id',
        'numberOfPartitions',
        'numberOfReplications',
      );
      expect(data.result.id).to.be.a('string');
      expect(data.result.numberOfPartitions).to.be.a('number');
      expect(data.result.numberOfReplications).to.be.a('number');
    });
  });
  it('fetchTopics', () => {
    cy.fetchTopics().then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.be.a('array');
      expect(data.result[0]).to.include.keys(
        'id',
        'numberOfPartitions',
        'numberOfReplications',
      );
      expect(data.result[0].id).to.be.a('string');
      expect(data.result[0].numberOfPartitions).to.be.a('number');
      expect(data.result[0].numberOfReplications).to.be.a('number');
    });
  });
});

describe('Pipelines Api test', () => {
  it.skip('createPipeline', () => {
    const data = {
      name: 'fakePipeline',
      rules: {},
      workerClusterName: wkName,
    };
    cy.testCreatePipeline(data).then(res => {
      const { data } = res;
      pipelineId = data.result.id;
      pipelineName = data.result.name;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys(
        'id',
        'name',
        'workerClusterName',
        'objects',
      );
      expect(data.result.id).to.be.a('string');
      expect(data.result.name).to.be.a('string');
      expect(data.result.workerClusterName).to.be.a('string');
      expect(data.result.objects).to.be.a('array');
    });
  });
  it.skip('fetchPipeline', () => {
    cy.fetchPipeline(pipelineId).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys(
        'id',
        'name',
        'workerClusterName',
        'objects',
      );
      expect(data.result.id).to.be.a('string');
      expect(data.result.name).to.be.a('string');
      expect(data.result.workerClusterName).to.be.a('string');
      expect(data.result.objects).to.be.a('array');
    });
  });
  it.skip('fetchPipelines', () => {
    cy.fetchPipelines().then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.be.a('array');
      expect(data.result[0]).to.include.keys(
        'id',
        'name',
        'workerClusterName',
        'objects',
      );
      expect(data.result[0].id).to.be.a('string');
      expect(data.result[0].name).to.be.a('string');
      expect(data.result[0].workerClusterName).to.be.a('string');
      expect(data.result[0].objects).to.be.a('array');
    });
  });
  it.skip('updatePipeline', () => {
    const data = {
      id: pipelineId,
      params: {
        name: pipelineName,
        rules: {
          [topicId]: [],
        },
        workerClusterName: wkName,
      },
    };
    cy.updatePipeline(data).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys(
        'id',
        'name',
        'workerClusterName',
        'objects',
        'rules',
      );
      expect(data.result.id).to.be.a('string');
      expect(data.result.name).to.be.a('string');
      expect(data.result.workerClusterName).to.be.a('string');
      expect(data.result.objects).to.be.a('array');
      expect(data.result.rules).to.be.a('object');
      expect(data.result.objects[0].id).to.eq(topicId);
      expect(data.result.objects[0].kind).to.eq('topic');
      expect(data.result.objects[0].name).to.eq(topicId);
    });
  });
  it.skip('deletePipeline', () => {
    cy.testDeletePipeline(pipelineId).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
    });
  });
});

describe('Connector Api test', () => {
  it('createConnector', () => {
    const data = {
      className: 'com.island.ohara.connector.ftp.FtpSource',
      configs: {},
      'connector.name': connectorName,
      name: connectorName,
      numberOfTasks: 1,
      schema: [],
      topics: [],
      workerClusterName: fakeWk,
    };
    cy.createConnector(data).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys('settings');
      expect(data.result.settings).to.include.keys(
        'className',
        'connector.name',
        'name',
        'tasks.max',
        'workerClusterName',
      );
      expect(data.result.settings).to.be.a('object');
      expect(data.result.settings.className).to.be.a('string');
      expect(data.result.settings['connector.name']).to.be.a('string');
      expect(data.result.settings['tasks.max']).to.be.a('number');
      expect(data.result.settings.name).to.be.a('string');
      expect(data.result.settings.workerClusterName).to.be.a('string');
    });
  });
  it('fetchConnector', () => {
    cy.fetchConnector(connectorName).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys('settings');
      expect(data.result.settings).to.include.keys(
        'className',
        'connector.name',
        'name',
        'tasks.max',
        'workerClusterName',
      );
      expect(data.result.settings).to.be.a('object');
      expect(data.result.settings['className']).to.be.a('string');
      expect(data.result.settings['connector.name']).to.be.a('string');
      expect(data.result.settings['tasks.max']).to.be.a('number');
      expect(data.result.settings.name).to.be.a('string');
      expect(data.result.settings.workerClusterName).to.be.a('string');
    });
  });
  it('updateConnector', () => {
    const data = {
      id: connectorName,
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
        topics: [topicId],
        version: '0.6-SNAPSHOT',
        workerClusterName: fakeWk,
      },
    };
    cy.updateConnector(data).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys('settings');
      expect(data.result.settings).to.be.a('object');
      expect(data.result.settings).to.include.keys(
        'author',
        'columns',
        'className',
        'connector.name',
        'ftp.completed.folder',
        'ftp.encode',
        'ftp.error.folder',
        'ftp.hostname',
        'ftp.input.folder',
        'ftp.port',
        'ftp.user.name',
        'ftp.user.password',
        'kind',
        'revision',
        'tasks.max',
        'topics',
        'version',
        'workerClusterName',
      );
      expect(data.result.settings.author).to.be.a('string');
      expect(data.result.settings.columns).to.be.a('array');
      expect(data.result.settings.className).to.be.a('string');
      expect(data.result.settings.name).to.be.a('string');
      expect(data.result.settings['connector.name']).to.be.a('string');
      expect(data.result.settings['ftp.completed.folder']).to.be.a('string');
      expect(data.result.settings['ftp.encode']).to.be.a('string');
      expect(data.result.settings['ftp.error.folder']).to.be.a('string');
      expect(data.result.settings['ftp.hostname']).to.be.a('string');
      expect(data.result.settings['ftp.input.folder']).to.be.a('string');
      expect(data.result.settings['ftp.port']).to.be.a('number');
      expect(data.result.settings['ftp.user.name']).to.be.a('string');
      expect(data.result.settings['ftp.user.password']).to.be.a('string');
      expect(data.result.settings.kind).to.be.a('string');
      expect(data.result.settings.revision).to.be.a('string');
      expect(data.result.settings['tasks.max']).to.be.a('number');
      expect(data.result.settings.topics).to.be.a('array');
      expect(data.result.settings.version).to.be.a('string');
      expect(data.result.settings.workerClusterName).to.be.a('string');
    });
  });
  it('startConnector', () => {
    cy.startConnector(connectorName).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys('id', 'settings', 'state');
      expect(data.result.state).to.be.a('string');
    });
  });
  it('stopConnector', () => {
    cy.stopConnector(connectorName).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys('id', 'settings');
    });
  });
  it('deleteConnector', () => {
    cy.deleteConnector(connectorName).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
    });
  });
});

describe('Streamapp Api test', () => {
  it('uploadStreamAppJar', () => {
    const params = {
      wk: wkName,
      jarName: 'ohara-streamapp.jar',
    };
    cy.testUploadStreamAppJar(params).then(res => {
      const { data } = res;
      streamAppId = data.result[0].id;
      expect(data.isSuccess).to.eq(true);
      expect(data.result[0]).to.include.keys('id', 'name', 'workerClusterName');
      expect(data.result[0].id).to.be.a('string');
      expect(data.result[0].name).to.be.a('string');
      expect(data.result[0].workerClusterName).to.be.a('string');
    });
  });
  it('fetchStreamAppJars', () => {
    cy.fetchStreamAppJars(wkName).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result[0]).to.include.keys('id', 'name', 'workerClusterName');
      expect(data.result[0].id).to.be.a('string');
      expect(data.result[0].name).to.be.a('string');
      expect(data.result[0].workerClusterName).to.be.a('string');
    });
  });
  it('createProperty', () => {
    const params = {
      jarId: streamAppId,
      name: 'Untitled streamApp',
    };
    cy.createProperty(params).then(res => {
      const { data } = res;
      propertyId = data.result.id;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys(
        'id',
        'instances',
        'jarInfo',
        'name',
        'from',
        'to',
        'workerClusterName',
      );
      expect(data.result.id).to.be.a('string');
      expect(data.result.instances).to.be.a('number');
      expect(data.result.name).to.be.a('string');
      expect(data.result.from).to.be.a('array');
      expect(data.result.to).to.be.a('array');
      expect(data.result.workerClusterName).to.be.a('string');
      expect(data.result.jarInfo).to.be.a('object');
      expect(data.result.jarInfo).to.include.keys('name', 'id');
    });
  });
  it('fetchProperty', () => {
    cy.fetchProperty(propertyId).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys(
        'id',
        'instances',
        'jarInfo',
        'name',
        'from',
        'to',
        'workerClusterName',
      );
      expect(data.result.id).to.be.a('string');
      expect(data.result.instances).to.be.a('number');
      expect(data.result.name).to.be.a('string');
      expect(data.result.from).to.be.a('array');
      expect(data.result.to).to.be.a('array');
      expect(data.result.workerClusterName).to.be.a('string');
      expect(data.result.jarInfo).to.be.a('object');
      expect(data.result.jarInfo).to.include.keys('name', 'id');
    });
  });
  it('updateProperty', () => {
    const params = {
      id: propertyId,
      jarId: streamAppId,
      name: 'test',
      from: [topicId],
      to: [],
      instances: 1,
    };
    cy.updateProperty(params).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys(
        'id',
        'instances',
        'jarInfo',
        'name',
        'from',
        'to',
        'workerClusterName',
      );
      expect(data.result.id).to.be.a('string');
      expect(data.result.instances).to.be.a('number');
      expect(data.result.name).to.be.a('string');
      expect(data.result.from).to.be.a('array');
      expect(data.result.to).to.be.a('array');
      expect(data.result.workerClusterName).to.be.a('string');
      expect(data.result.jarInfo).to.be.a('object');
      expect(data.result.jarInfo).to.include.keys('name', 'id');
      expect(data.result.name).to.eq('test');
      expect(data.result.from[0]).to.eq(topicId);
    });
  });
  it('stopStreamApp', () => {
    cy.stopStreamApp(propertyId).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
    });
  });
  it('deleteProperty', () => {
    cy.stopStreamApp(propertyId).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
    });
  });
  it('deleteStreamAppJar', () => {
    cy.deleteStreamAppJar(streamAppId).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
    });
  });
});

describe('Log Api test', () => {
  it('fetchLogs', () => {
    cy.fetchLogs('workers', wkName).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys('name', 'logs');
      expect(data.result.name).to.be.a('string');
      expect(data.result.logs).to.be.a('array');
      expect(data.result.logs[0]).to.include.keys('name', 'value');
      expect(data.result.logs[0].name).to.be.a('string');
      expect(data.result.logs[0].value).to.be.a('string');
    });
  });
});

describe('Validate Api test', () => {
  it('validateConnector', () => {
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
      topics: [topicId],
      version: '0.6-SNAPSHOT',
      workerClusterName: fakeWk,
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
