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

const generateNode = () => {
  const params = {
    hostname: generate.name({ prefix: 'node' }),
    port: generate.port(),
    user: generate.userName(),
    password: generate.password(),
  };

  cy.log('current using node name:' + params.hostname);
  return params;
};

describe('Node API', () => {
  beforeEach(() => cy.deleteAllServices());

  it('createNode', () => {
    const params = generateNode();
    cy.createNode(params).then(response => {
      const {
        data: { isSuccess, result },
      } = response;
      const {
        hostname,
        port,
        user,
        password,
        lastModified,
        tags,
        services,
      } = result;

      expect(isSuccess).to.eq(true);

      expect(hostname).to.be.a('string');
      expect(hostname).to.eq(params.hostname);

      expect(port).to.be.a('number');
      expect(port).to.eq(params.port);

      expect(user).to.be.a('string');
      expect(user).to.eq(params.user);

      expect(password).to.be.a('string');
      expect(password).to.eq(params.password);

      expect(services).to.be.an('array');

      expect(lastModified).to.be.a('number');

      expect(tags).to.be.an('object');

      services.forEach(service => {
        expect(service.name).to.be.a('string');
        expect(service.clusterKeys).to.be.an('array');
      });
    });
  });

  it('updateNode', () => {
    const params = generateNode();
    cy.createNode(params);

    let newParams = Object.assign({}, params);
    newParams.user = generate.userName();
    newParams.password = generate.password();
    newParams.tags = { a: 'tag' };

    cy.updateNode(newParams).then(response => {
      const {
        data: { isSuccess, result },
      } = response;
      const {
        hostname,
        port,
        user,
        password,
        lastModified,
        tags,
        services,
      } = result;

      expect(isSuccess).to.eq(true);

      expect(hostname).to.be.a('string');
      expect(hostname).to.eq(params.hostname);

      expect(port).to.be.a('number');
      expect(port).to.eq(params.port);

      expect(services).to.be.an('array');

      expect(lastModified).to.be.a('number');

      expect(tags).to.be.an('object');
      expect(tags.a).to.eq('tag');

      // we update the user and password with newParams
      expect(user).to.be.a('string');
      expect(user).to.eq(newParams.user);

      expect(password).to.be.a('string');
      expect(password).to.eq(newParams.password);
    });
  });

  it('fetchNode', () => {
    const params = generateNode();
    cy.createNode(params);

    cy.fetchNode(params).then(response => {
      const {
        data: { isSuccess, result },
      } = response;
      const {
        hostname,
        port,
        user,
        password,
        lastModified,
        tags,
        services,
      } = result;

      expect(isSuccess).to.eq(true);

      expect(hostname).to.be.a('string');
      expect(hostname).to.eq(params.hostname);

      expect(port).to.be.a('number');
      expect(port).to.eq(params.port);

      expect(user).to.be.a('string');
      expect(user).to.eq(params.user);

      expect(password).to.be.a('string');
      expect(password).to.eq(params.password);

      expect(services).to.be.an('array');

      expect(lastModified).to.be.a('number');

      expect(tags).to.be.an('object');
    });

    cy.deleteNode(params);
  });

  it('fetchNodes', () => {
    const paramsOne = generateNode();
    const paramsTwo = generateNode();

    cy.createNode(paramsOne);
    cy.createNode(paramsTwo);

    cy.fetchNodes().then(response => {
      const {
        data: { isSuccess, result },
      } = response;

      expect(isSuccess).to.eq(true);

      const nodes = result.filter(
        node =>
          node.hostname === paramsOne.hostname ||
          node.hostname === paramsTwo.hostname,
      );

      expect(nodes.length).to.eq(2);
    });
  });

  it('deleteNode', () => {
    const params = generateNode();
    cy.createNode(params);
    cy.deleteNode(params);

    cy.fetchNodes().then(response => {
      const {
        data: { isSuccess, result },
      } = response;

      expect(isSuccess).to.eq(true);

      const nodes = result.filter(node => node.hostname === params.hostname);

      expect(nodes.length).to.eq(0);
    });
  });
});
