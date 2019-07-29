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

let jar = {};
let jarGroup = '';
let propertyName = '';
const nodeName = `node${makeRandomPort()}`;
const zookeeperName = `zk${makeRandomPort()}`;

describe('Node API', () => {
  before(() => cy.deleteAllServices());

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

describe('Zookeeper API', () => {
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

describe('Jar API', () => {
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

describe('Stream API', () => {
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
      from: [],
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
