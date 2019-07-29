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

describe('Container API', () => {
  let nodeName = '';
  let zookeeperClusterName = '';
  let brokerClusterName = '';
  let workerClusterName = '';

  before(() => cy.deleteAllServices());

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
    });
  });

  it('fetchContainers', () => {
    cy.fetchContainers(workerClusterName).then(res => {
      const {
        data: { isSuccess, result },
      } = res;

      expect(isSuccess).to.eq(true);
      expect(result).to.be.a('array');
      expect(result[0]).include.keys('containers');
      expect(result[0].containers).to.be.a('array');
      expect(result[0].containers[0]).include.keys('state');
      expect(result[0].containers[0].state).to.be.a('string');
    });
  });
});
