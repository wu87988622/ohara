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

const setup = () => {
  const nodeName = `node${utils.makeRandomStr()}`;
  const zookeeperClusterName = `zookeeper${utils.makeRandomStr()}`;
  const brokerClusterName = `broker${utils.makeRandomStr()}`;

  cy.createNode({
    name: nodeName,
    port: 22,
    user: utils.makeRandomStr(),
    password: utils.makeRandomStr(),
  });

  cy.createZookeeper({
    name: zookeeperClusterName,
    nodeNames: [nodeName],
  });

  cy.startZookeeper(zookeeperClusterName);

  cy.createBroker({
    name: brokerClusterName,
    nodeNames: [nodeName],
    zookeeperClusterName,
  }).as('createBroker');

  return {
    nodeName,
    zookeeperClusterName,
    brokerClusterName,
  };
};

describe('Broker API', () => {
  beforeEach(() => cy.deleteAllServices());

  it('createBroker', () => {
    const { brokerClusterName } = setup();

    cy.get('@createBroker').then(response => {
      const {
        data: { isSuccess, result },
      } = response;
      const {
        name,
        nodeNames,
        clientPort,
        exporterPort,
        jmxPort,
        state,
      } = result;

      expect(isSuccess).to.eq(true);

      expect(name).to.eq(brokerClusterName);
      expect(nodeNames)
        .to.be.an('array')
        .that.have.lengthOf(1);
      expect(clientPort).to.be.a('number');
      expect(exporterPort).to.be.a('number');
      expect(jmxPort).to.be.a('number');
      expect(state).to.be.undefined;
    });
  });

  it('fetchBroker', () => {
    const { brokerClusterName } = setup();

    cy.fetchBroker(brokerClusterName).then(response => {
      const {
        data: { isSuccess, result },
      } = response;
      const {
        name,
        nodeNames,
        clientPort,
        exporterPort,
        jmxPort,
        state,
      } = result;

      expect(isSuccess).to.eq(true);
      expect(name).to.eq(brokerClusterName);
      expect(nodeNames)
        .to.be.an('array')
        .that.have.lengthOf(1);
      expect(clientPort).to.be.a('number');
      expect(exporterPort).to.be.a('number');
      expect(jmxPort).to.be.a('number');
      expect(state).to.be.undefined;
    });
  });

  it('fetchBrokers', () => {
    const { nodeName, zookeeperClusterName } = setup();

    const paramsOne = {
      name: utils.makeRandomStr(),
      zookeeperClusterName,
      nodeNames: [nodeName],
    };

    const paramsTwo = {
      name: utils.makeRandomStr(),
      zookeeperClusterName,
      nodeNames: [nodeName],
    };

    cy.createBroker(paramsOne);
    cy.createBroker(paramsTwo);

    cy.fetchBrokers().then(res => {
      const {
        data: { isSuccess, result },
      } = res;

      expect(isSuccess).to.eq(true);

      const brokers = result.filter(
        broker =>
          broker.name === paramsOne.name || broker.name === paramsTwo.name,
      );

      expect(brokers.length).to.eq(2);
    });
  });

  it('startBroker', () => {
    const { zookeeperClusterName, brokerClusterName } = setup();

    cy.startZookeeper(zookeeperClusterName);

    cy.fetchBroker(brokerClusterName).then(response => {
      expect(response.state).to.be.undefined;
    });

    cy.startBroker(brokerClusterName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });

    cy.fetchBroker(brokerClusterName).then(response => {
      expect(response.data.result.state).to.eq('RUNNING');
    });
  });

  it('stopBroker', () => {
    const { zookeeperClusterName, brokerClusterName } = setup();

    cy.startZookeeper(zookeeperClusterName);

    cy.fetchBroker(brokerClusterName).then(response => {
      expect(response.state).to.be.undefined;
    });

    cy.startBroker(brokerClusterName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });

    cy.fetchBroker(brokerClusterName).then(response => {
      expect(response.data.result.state).to.eq('RUNNING');
    });

    cy.stopBroker(brokerClusterName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });

    cy.fetchBroker(brokerClusterName).then(response => {
      expect(response.data.result.state).to.eq.undefined;
    });
  });

  it('deleteBroker', () => {
    const { brokerClusterName } = setup();

    cy.fetchBroker(brokerClusterName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });

    cy.deleteBroker(brokerClusterName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });

    cy.fetchBrokers().then(response => {
      const targetBroker = response.data.result.find(
        broker => broker.name === brokerClusterName,
      );

      expect(targetBroker).to.be.undefined;
    });
  });

  it('forceDeleteBroker', () => {
    const { brokerClusterName } = setup();

    cy.fetchBroker(brokerClusterName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });

    // We're not currently using this API in the client, and so it's not
    // listed in the brokerApi.js, so we're asserting the response status
    // not the isSuccess value
    cy.request('DELETE', `api/brokers/${brokerClusterName}?force=true`).then(
      response => {
        expect(response.status).to.eq(204);
      },
    );

    cy.fetchBrokers().then(response => {
      const targetBroker = response.data.result.find(
        broker => broker.name === brokerClusterName,
      );

      expect(targetBroker).to.be.undefined;
    });
  });
});
