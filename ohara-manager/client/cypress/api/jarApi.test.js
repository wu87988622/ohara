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

const setup = () => {
  const jarName = 'ohara-it-source.jar';
  cy.createJar(jarName).as('createJar');

  return {
    jarName,
  };
};

describe('Jar API', () => {
  before(() => cy.deleteAllServices());

  it('createJar', () => {
    const { jarName } = setup();

    cy.get('@createJar').then(response => {
      const {
        data: { result, isSuccess },
      } = response;

      const { name, group, size, lastModified, tags } = result;

      expect(isSuccess).to.eq(true);

      expect(name).to.eq(jarName);
      expect(size).to.be.a('number');
      expect(group).to.be.a('string');
      expect(lastModified).to.be.a('number');
      expect(tags.name).to.eq(jarName);
    });
  });

  it('fetchJars', () => {
    setup();

    cy.get('@createJar').then(response => {
      const { group } = response.data.result;

      cy.fetchJars(group).then(response => {
        const {
          data: { result, isSuccess },
        } = response;

        expect(isSuccess).to.eq(true);
        expect(result).to.be.a('array');
        expect(result[0]).to.include.keys('name', 'group');
        expect(result[0].name).to.be.a('string');
        expect(result[0].group).to.be.a('string');
        expect(result[0].tags.name).to.eq(result[0].name);
      });
    });
  });
});
