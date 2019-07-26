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
describe('Broker API', () => {
  let nodeName = '';
  let zookeeperClusterName = '';
  let brokerClusterName = '';

  beforeEach(() => {
    nodeName = `node${utils.makeRandomStr()}`;
    zookeeperClusterName = `zookeeper${utils.makeRandomStr()}`;
    brokerClusterName = `broker${utils.makeRandomStr()}`;

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

    cy.createBroker({
      name: brokerClusterName,
      zookeeperClusterName: zookeeperClusterName,
      nodeNames: [nodeName],
    }).as('createBroker');
  });

  it('createBroker', () => {
    cy.get('@createBroker').then(response => {
      const {
        data: { isSuccess, result },
      } = response;
      const { name, nodeNames, clientPort, exporterPort, jmxPort } = result;

      expect(isSuccess).to.eq(true);

      expect(name).to.eq(brokerClusterName);
      expect(nodeNames)
        .to.be.an('array')
        .that.have.lengthOf(1);
      expect(clientPort).to.be.a('number');
      expect(exporterPort).to.be.a('number');
      expect(jmxPort).to.be.a('number');
    });
  });

  it('fetchBroker', () => {
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
    const paramsOne = {
      name: utils.makeRandomStr(),
      nodeNames: [nodeName],
    };

    const paramsTwo = {
      name: utils.makeRandomStr(),
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
});
