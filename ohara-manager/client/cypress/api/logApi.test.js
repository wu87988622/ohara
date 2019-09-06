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

describe('Log API', () => {
  beforeEach(() => cy.deleteAllServices());

  it('fetchLogs', () => {
    const nodeName = generate.serviceName({ prefix: 'node' });
    const zookeeperClusterName = generate.serviceName({ prefix: 'zookeeper' });
    const brokerClusterName = generate.serviceName({ prefix: 'broker' });
    const workerClusterName = generate.serviceName({ prefix: 'worker' });

    cy.createNode({
      name: nodeName,
      port: generate.port(),
      user: generate.userName(),
      password: generate.password(),
    });

    cy.createZookeeper({
      nodeNames: [nodeName],
      name: zookeeperClusterName,
    });

    cy.startZookeeper(zookeeperClusterName);

    cy.createBroker({
      name: brokerClusterName,
      nodeNames: [nodeName],
      zookeeperClusterName,
    });

    cy.startBroker(brokerClusterName);

    cy.createWorker({
      name: workerClusterName,
      nodeNames: [nodeName],
      brokerClusterName,
    });

    cy.startWorker(workerClusterName);

    cy.fetchLogs('workers', workerClusterName).then(response => {
      const {
        data: { isSuccess, result },
      } = response;
      const { name, logs } = result;

      expect(isSuccess).to.eq(true);

      expect(name).to.be.a('string');
      expect(logs).to.be.a('array');
      expect(logs[0].name).to.be.a('string');
      expect(logs[0].value).to.be.a('string');
    });
  });
});
