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

import * as utils from '../utils';

/* eslint-disable no-unused-expressions */
// eslint is complaining about `expect(thing).to.be.undefined`
describe('Zookeeper API', () => {
  let nodeName = '';
  let zookeeperClusterName = '';

  before(() => cy.deleteAllServices());

  beforeEach(() => {
    nodeName = `node${utils.makeRandomStr()}`;
    zookeeperClusterName = `zookeeper${utils.makeRandomStr()}`;

    cy.createNode({
      name: nodeName,
      port: 22,
      user: utils.makeRandomStr(),
      password: utils.makeRandomStr(),
    });

    cy.createZookeeper({
      name: zookeeperClusterName,
      nodeNames: [nodeName],
    }).as('createZookeeper');
  });

  it('createZookeeper', () => {
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
      } = result;

      expect(isSuccess).to.eq(true);

      expect(name).to.eq(zookeeperClusterName);
      expect(nodeNames)
        .to.be.an('array')
        .that.have.lengthOf(1);
      expect(clientPort).to.be.a('number');
      expect(electionPort).to.be.a('number');
      expect(peerPort).to.be.a('number');
      expect(state).to.be.undefined;
    });
  });

  it('fetchZookeeper', () => {
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
      } = result;

      expect(isSuccess).to.eq(true);

      expect(name).to.eq(zookeeperClusterName);
      expect(nodeNames)
        .to.be.an('array')
        .that.have.lengthOf(1);
      expect(clientPort).to.be.a('number');
      expect(electionPort).to.be.a('number');
      expect(peerPort).to.be.a('number');
      expect(state).to.be.undefined;
    });
  });

  it('fetchZookeepers', () => {
    const paramsOne = {
      name: utils.makeRandomStr(),
      nodeNames: [nodeName],
    };

    const paramsTwo = {
      name: utils.makeRandomStr(),
      nodeNames: [nodeName],
    };

    cy.createZookeeper(paramsOne);
    cy.createZookeeper(paramsTwo);

    cy.fetchZookeepers().then(res => {
      const {
        data: { isSuccess, result },
      } = res;

      expect(isSuccess).to.eq(true);

      const zookeepers = result.filter(
        zookeeper =>
          zookeeper.name === paramsOne.name ||
          zookeeper.name === paramsTwo.name,
      );

      expect(zookeepers.length).to.eq(2);
    });
  });

  it('startZookeeper', () => {
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

  it('stopBroker', () => {
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

  it('deleteZookeeper', () => {
    cy.fetchZookeeper(zookeeperClusterName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });

    cy.deleteZookeeper(zookeeperClusterName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });

    cy.fetchZookeepers().then(response => {
      const targetZookeeper = response.data.result.find(
        zookeeper => zookeeper.name === zookeeperClusterName,
      );

      expect(targetZookeeper).to.be.undefined;
    });
  });

  it('forceDeleteBroker', () => {
    cy.fetchZookeeper(zookeeperClusterName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });

    // We're not currently using this API in the client, and so it's not
    // listed in the brokerApi.js, so we're asserting the response status
    // not the isSuccess value
    cy.request(
      'DELETE',
      `api/zookeepers/${zookeeperClusterName}?force=true`,
    ).then(response => {
      expect(response.status).to.eq(204);
    });

    cy.fetchZookeepers().then(response => {
      const targetZookeeper = response.data.result.find(
        zookeeper => zookeeper.name === zookeeperClusterName,
      );

      expect(targetZookeeper).to.be.undefined;
    });
  });
});
