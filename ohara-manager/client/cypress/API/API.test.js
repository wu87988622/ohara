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
let jarID = '';
let pipelineID = '';
const wkName = `wk${makeRandomPort()}`;

describe('Zookeeper Api test', () => {
  it('fetchZookeepers', () => {
    cy.fetchZookeepers().then(res => {
      const data = res.data;
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
      const data = res.data;
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
      const data = res.data;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys('name', 'id');
      expect(data.result.name).to.be.a('string');
      expect(data.result.id).to.be.a('string');
    });
  });
  it('fetchJars', () => {
    cy.fetchJars().then(res => {
      const data = res.data;
      jarID = data.result[0].id;
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
      nodeNames: [Cypress.env('nodeHost')],
      plugins: [jarID],
    };
    cy.testCreateWorker(data).then(res => {
      const data = res.data;
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
      const data = res.data;
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
      const data = res.data;
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
      const data = res.data;
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
      const data = res.data;
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
      const data = res.data;
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

describe('Node Api test', () => {
  const nodeName = `node${makeRandomPort()}`;
  it('createNode', () => {
    const data = {
      name: nodeName,
      port: 22,
      user: 'ohara',
      password: '123',
    };
    cy.createNode(data).then(res => {
      const data = res.data;
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
      const data = res.data;
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
      const data = res.data;
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
