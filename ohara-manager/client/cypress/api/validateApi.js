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

const setup = () => {
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
    name: zookeeperClusterName,
    nodeNames: [nodeName],
  });

  cy.startZookeeper(zookeeperClusterName);

  cy.createBroker({
    name: brokerClusterName,
    zookeeperClusterName,
    nodeNames: [nodeName],
  });

  cy.startBroker(brokerClusterName);

  cy.createWorker({
    name: workerClusterName,
    brokerClusterName,
    nodeNames: [nodeName],
  });

  cy.startWorker(workerClusterName);

  return {
    workerClusterName,
  };
};

describe('Validate API', () => {
  before(() => cy.deleteAllServices());

  it('validateConnector', () => {
    const { workerClusterName } = setup();

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
      topics: ['topicName'],
      version: '0.8.0-SNAPSHOT',
      workerClusterName,
    };

    cy.validateConnector(params).then(response => {
      cy.log(response);
      const {
        data: { isSuccess, result },
      } = response;

      const { errorCount, settings } = result;

      expect(isSuccess).to.eq(true);

      expect(errorCount).to.eq(0);
      expect(settings).to.be.a('array');
    });
  });
});
