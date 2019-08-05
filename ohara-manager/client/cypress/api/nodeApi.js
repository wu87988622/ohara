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

const setup = () => {
  const nodeName = `node${utils.makeRandomStr()}`;

  cy.createNode({
    name: nodeName,
    port: 22,
    user: utils.makeRandomStr(),
    password: utils.makeRandomStr(),
  }).as('createNode');

  return { nodeName };
};

describe('Node API', () => {
  beforeEach(() => cy.deleteAllServices());

  it('createNode', () => {
    const { nodeName } = setup();

    cy.get('@createNode').then(response => {
      const {
        data: { isSuccess, result },
      } = response;
      const { services, name, password, port, user, lastModified } = result;
      const [zookeeper, broker, worker] = services;

      expect(isSuccess).to.eq(true);

      expect(services).to.be.a('array');
      expect(name).to.eq(nodeName);
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
    const { nodeName } = setup();

    const params = {
      name: nodeName,
      port: utils.makeRandomPort(),
      user: utils.makeRandomStr(),
      password: utils.makeRandomStr(),
    };

    cy.updateNode(params).then(response => {
      const {
        data: { isSuccess, result },
      } = response;
      const { services, name, password, port, user, lastModified } = result;
      const [zookeeper, broker, worker] = services;

      expect(isSuccess).to.eq(true);

      expect(services).to.be.a('array');
      expect(name).to.be.a('string');
      expect(port).to.eq(params.port);
      expect(user).to.eq(params.user);
      expect(password).to.eq(params.password);
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
    const paramsOne = {
      name: utils.makeRandomStr(),
      port: 22,
      user: utils.makeRandomStr(),
      password: utils.makeRandomStr(),
    };

    const paramsTwo = {
      name: utils.makeRandomStr(),
      port: 22,
      user: utils.makeRandomStr(),
      password: utils.makeRandomStr(),
    };

    cy.createNode(paramsOne);
    cy.createNode(paramsTwo);

    cy.fetchNodes().then(response => {
      const {
        data: { isSuccess, result },
      } = response;

      expect(isSuccess).to.eq(true);

      const nodes = result.filter(
        node => node.name === paramsOne.name || node.name === paramsTwo.name,
      );

      expect(nodes.length).to.eq(2);
    });
  });
});
