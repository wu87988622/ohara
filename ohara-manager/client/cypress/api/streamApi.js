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
  let streamName = `stream${utils.makeRandomStr()}`;
  cy.createJar('ohara-it-source.jar').then(response => {
    const params = {
      jar: {
        name: response.data.result.name,
        group: response.data.result.group,
      },
      name: streamName,
    };

    cy.createProperty(params)
      .then(response => response)
      .as('createProperty');
  });

  return {
    streamName,
  };
};

describe('Stream property API', () => {
  beforeEach(() => cy.deleteAllServices());

  it('createProperty', () => {
    cy.createJar('ohara-it-source.jar').then(response => {
      const params = {
        jar: {
          name: response.data.result.name,
          group: response.data.result.group,
        },
        name: `stream${utils.makeRandomStr()}`,
      };

      cy.createProperty(params).then(response => {
        const {
          data: { isSuccess, result },
        } = response;
        const { instances, name, from, to, jar } = result;

        expect(isSuccess).to.eq(true);

        expect(name).to.be.a('string');
        expect(instances).to.be.a('number');
        expect(from).to.be.a('array');
        expect(to).to.be.a('array');
        expect(jar).to.be.a('object');
        expect(jar).to.include.keys('name', 'group');
      });
    });
  });

  it('fetchProperty', () => {
    const { streamName } = setup();

    cy.fetchProperty(streamName).then(response => {
      const {
        data: { isSuccess, result },
      } = response;
      const { instances, name, from, to, jar } = result;

      expect(isSuccess).to.eq(true);

      expect(name).to.eq(streamName);
      expect(instances).to.be.a('number');
      expect(from).to.be.a('array');
      expect(to).to.be.a('array');
      expect(jar).to.be.a('object');
      expect(jar).to.include.keys('name', 'group');
    });
  });

  it('updateProperty', () => {
    const { streamName } = setup();

    const params = {
      name: streamName,
      from: [],
      instances: 1,
    };

    cy.updateProperty(params).then(response => {
      const {
        data: { isSuccess, result },
      } = response;
      const { instances, name, from, to, jar } = result;

      expect(isSuccess).to.eq(true);

      expect(name).to.eq(streamName);
      expect(instances).to.be.a('number');
      expect(from).to.be.a('array');
      expect(to).to.be.a('array');
      expect(jar).to.be.a('object');
      expect(jar).to.include.keys('name', 'group');
    });
  });

  it('stopStreamApp', () => {
    const { streamName } = setup();

    cy.stopStreamApp(streamName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });
  });

  it('deleteProperty', () => {
    const { streamName } = setup();

    cy.deleteProperty(streamName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });
  });
});
