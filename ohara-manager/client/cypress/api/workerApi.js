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

describe('Workers', () => {
  let nodeName = '';
  let zookeeperClusterName = '';
  let brokerClusterName = '';
  let workerClusterName = '';

  beforeEach(() => {
    nodeName = `node${utils.makeRandomStr()}`;
    zookeeperClusterName = `zookeeper${utils.makeRandomStr()}`;
    brokerClusterName = `broker${utils.makeRandomStr()}`;
    workerClusterName = `worker${utils.makeRandomStr()}`;

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
      zookeeperClusterName: zookeeperClusterName,
      nodeNames: [nodeName],
    });

    cy.startBroker(brokerClusterName);

    cy.testCreateWorker({
      name: workerClusterName,
      brokerClusterName,
      nodeNames: [nodeName],
    }).as('testCreateWorker');
  });

  it('createWorker', () => {
    cy.get('@testCreateWorker').then(res => {
      const {
        data: { isSuccess, result },
      } = res;

      expect(isSuccess).to.eq(true);

      expect(result.name).to.eq(workerClusterName);
      expect(result.clientPort).to.be.a('number');
      expect(result.nodeNames)
        .to.be.a('array')
        .that.have.lengthOf(1);
      expect(result.connectors).to.be.a('array');
      expect(result.jarInfos).to.be.a('array');
      expect(result.configTopicName).to.be.a('string');
      expect(result.offsetTopicName).to.be.a('string');
      expect(result.statusTopicName).to.be.a('string');
    });
  });

  it('fetchWorker', () => {
    cy.fetchWorker(workerClusterName).then(res => {
      const {
        data: { isSuccess, result },
      } = res;

      expect(isSuccess).to.eq(true);

      expect(result.name).to.eq(workerClusterName);
      expect(result.clientPort).to.be.a('number');
      expect(result.nodeNames).to.be.a('array');
      expect(result.connectors).to.be.a('array');
      expect(result.jarInfos).to.be.a('array');
    });
  });

  it('fetchWorkers', () => {
    const paramsOne = {
      name: utils.makeRandomStr(),
      brokerClusterName,
      nodeNames: [nodeName],
    };

    const paramsTwo = {
      name: utils.makeRandomStr(),
      brokerClusterName,
      nodeNames: [nodeName],
    };

    cy.testCreateWorker(paramsOne);
    cy.testCreateWorker(paramsTwo);

    cy.fetchWorkers().then(res => {
      const {
        data: { isSuccess, result },
      } = res;

      expect(isSuccess).to.eq(true);

      const workers = result.filter(
        worker =>
          worker.name === paramsOne.name || worker.name === paramsTwo.name,
      );

      expect(workers.length).to.eq(2);
    });
  });
});
