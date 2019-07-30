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

let jar = {};
let jarGroup = '';
let propertyName = '';

describe('Jar API', () => {
  const testJarName = 'ohara-it-source.jar';

  it('createJar', () => {
    cy.createJar(testJarName).then(res => {
      const { data } = res;
      jar = {
        name: data.result.name,
        group: data.result.group,
      };
      jarGroup = data.result.group;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys('name', 'group');
      expect(data.result.name).to.be.a('string');
      expect(data.result.group).to.be.a('string');
    });
  });

  it('fetchJars', () => {
    cy.fetchJars(jarGroup).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.be.a('array');
      expect(data.result[0]).to.include.keys('name', 'group');
      expect(data.result[0].name).to.be.a('string');
      expect(data.result[0].group).to.be.a('string');
    });
  });
});

describe('Stream API', () => {
  it('createProperty', () => {
    const params = {
      jar: jar,
      name: 'streamapp',
    };

    cy.createProperty(params).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { instances, name, from, to, jar } = result;

      propertyName = name;

      expect(isSuccess).to.eq(true);

      expect(name).to.be.a('string');
      expect(instances).to.be.a('number');
      expect(from).to.be.a('array');
      expect(to).to.be.a('array');
      expect(jar).to.be.a('object');
      expect(jar).to.include.keys('name', 'group');
    });
  });

  it('fetchProperty', () => {
    cy.fetchProperty(propertyName).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { instances, name, from, to, jar } = result;

      expect(isSuccess).to.eq(true);

      expect(instances).to.be.a('number');
      expect(name).to.be.a('string');
      expect(from).to.be.a('array');
      expect(to).to.be.a('array');
      expect(jar).to.be.a('object');
      expect(jar).to.include.keys('name', 'group');
    });
  });

  it('updateProperty', () => {
    const params = {
      name: propertyName,
      from: [],
      instances: 1,
    };

    cy.updateProperty(params).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { instances, name, from, to, jar } = result;

      expect(isSuccess).to.eq(true);

      expect(instances).to.be.a('number');
      expect(name).to.be.a('string');
      expect(from).to.be.a('array');
      expect(to).to.be.a('array');
      expect(jar).to.be.a('object');
      expect(jar).to.include.keys('name', 'group');
    });
  });

  it('stopStreamApp', () => {
    cy.stopStreamApp(propertyName).then(res => {
      expect(res.data.isSuccess).to.eq(true);
    });
  });

  it('deleteProperty', () => {
    cy.deleteProperty(propertyName).then(res => {
      expect(res.data.isSuccess).to.eq(true);
    });
  });
});
