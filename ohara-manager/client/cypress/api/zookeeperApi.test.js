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

import * as generate from '../../src/utils/generate';

/* eslint-disable no-unused-expressions */
// eslint is complaining about `expect(thing).to.be.undefined`

const setup = () => {
  const nodeName = generate.serviceName({ prefix: 'node' });
  const zookeeperClusterName = generate.serviceName({ prefix: 'zk' });

  cy.createNode({
    name: nodeName,
    port: generate.port(),
    user: generate.userName(),
    password: generate.password(),
  });

  cy.createZookeeper({
    name: zookeeperClusterName,
    nodeNames: [nodeName],
    tags: {
      name: zookeeperClusterName,
    },
  }).as('createZookeeper');

  return {
    nodeName,
    zookeeperClusterName,
  };
};

describe('Zookeeper API', () => {
  beforeEach(() => cy.deleteAllServices());

  it('createZookeeper', () => {
    const { zookeeperClusterName } = setup();

    cy.get('@createZookeeper').then(response => {
      const {
        data: { isSuccess, result },
      } = response;
      const {
        name,
        nodeNames,
        clientPort,
        electionPort,
        peerPort,
        state,
        imageName,
        tags,
      } = result.settings;

      expect(isSuccess).to.eq(true);

      expect(name).to.eq(zookeeperClusterName);
      expect(nodeNames)
        .to.be.an('array')
        .that.have.lengthOf(1);
      expect(clientPort).to.be.a('number');
      expect(electionPort).to.be.a('number');
      expect(peerPort).to.be.a('number');
      expect(state).to.be.undefined;
      expect(imageName).to.be.a('string');
      expect(tags.name).to.eq(zookeeperClusterName);
    });
  });

  it('fetchZookeeper', () => {
    const { zookeeperClusterName } = setup();

    cy.fetchZookeeper(zookeeperClusterName).then(response => {
      const {
        data: { isSuccess, result },
      } = response;
      const {
        name,
        nodeNames,
        clientPort,
        electionPort,
        peerPort,
        state,
        imageName,
        tags,
      } = result.settings;

      expect(isSuccess).to.eq(true);

      expect(name).to.eq(zookeeperClusterName);
      expect(nodeNames)
        .to.be.an('array')
        .that.have.lengthOf(1);
      expect(clientPort).to.be.a('number');
      expect(electionPort).to.be.a('number');
      expect(peerPort).to.be.a('number');
      expect(state).to.be.undefined;
      expect(imageName).to.be.a('string');
      expect(tags.name).to.eq(zookeeperClusterName);
    });
  });

  it('fetchZookeepers', () => {
    const { nodeName } = setup();

    const paramsOne = {
      name: generate.serviceName({ prefix: 'zookeeper' }),
      nodeNames: [nodeName],
    };

    const paramsTwo = {
      name: generate.serviceName({ prefix: 'zookeeper' }),
      nodeNames: [nodeName],
    };

    cy.createZookeeper(paramsOne);
    cy.createZookeeper(paramsTwo);

    cy.fetchZookeepers().then(response => {
      const {
        data: { isSuccess, result },
      } = response;

      expect(isSuccess).to.eq(true);

      const zookeepers = result.filter(
        zookeeper =>
          zookeeper.settings.name === paramsOne.name ||
          zookeeper.settings.name === paramsTwo.name,
      );

      expect(zookeepers.length).to.eq(2);
    });
  });

  it('startZookeeper', () => {
    const { zookeeperClusterName } = setup();

    cy.fetchZookeeper(zookeeperClusterName).then(response => {
      expect(response.state).to.be.undefined;
    });

    cy.startZookeeper(zookeeperClusterName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });

    cy.fetchZookeeper(zookeeperClusterName).then(response => {
      expect(response.data.result.state).to.eq('RUNNING');
    });
  });

  it('stopZookeeper', () => {
    const { zookeeperClusterName } = setup();

    cy.fetchZookeeper(zookeeperClusterName).then(response => {
      expect(response.state).to.be.undefined;
    });

    cy.startZookeeper(zookeeperClusterName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });

    cy.fetchZookeeper(zookeeperClusterName).then(response => {
      expect(response.data.result.state).to.eq('RUNNING');
    });

    cy.stopZookeeper(zookeeperClusterName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });

    cy.fetchZookeeper(zookeeperClusterName).then(response => {
      expect(response.data.result.state).to.eq.undefined;
    });
  });

  it('forceStopZookeeper', () => {
    const { zookeeperClusterName } = setup();

    cy.fetchZookeeper(zookeeperClusterName).then(response => {
      expect(response.state).to.be.undefined;
    });

    cy.startZookeeper(zookeeperClusterName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });

    cy.fetchZookeeper(zookeeperClusterName).then(response => {
      expect(response.data.result.state).to.eq('RUNNING');
    });

    // This API is not actually used in the UI, but used in our E2E start scripts
    cy.request(
      'PUT',
      `api/workers/${zookeeperClusterName}/stop?force=true`,
    ).then(response => {
      expect(response.status).to.eq(202);
    });

    cy.fetchZookeeper(zookeeperClusterName).then(response => {
      expect(response.data.result.state).to.eq.undefined;
    });
  });

  it('deleteZookeeper', () => {
    const { zookeeperClusterName } = setup();

    cy.fetchZookeeper(zookeeperClusterName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });

    cy.deleteZookeeper(zookeeperClusterName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });

    cy.fetchZookeepers().then(response => {
      const targetZookeeper = response.data.result.find(
        zookeeper => zookeeper.settings.name === zookeeperClusterName,
      );

      expect(targetZookeeper).to.be.undefined;
    });
  });
});
