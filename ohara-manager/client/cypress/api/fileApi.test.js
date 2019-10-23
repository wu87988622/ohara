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

const generateFile = () => {
  const params = {
    // we use an existing file to simulate upload jar
    name: 'ohara-it-source.jar',
    group: generate.name({ prefix: 'group' }),
  };
  return params;
};

describe('File API', () => {
  beforeEach(() => cy.deleteAllServices());

  it('uploadJar', () => {
    const file = generateFile();
    cy.createJar(file.name, file.group).then(response => {
      const {
        data: { isSuccess, result },
      } = response;
      const { group, name, size, url, lastModified, tags } = result;

      expect(isSuccess).to.eq(true);

      expect(group).to.be.a('string');
      expect(group).to.eq(file.group);

      expect(name).to.be.a('string');
      expect(name).to.eq(file.name);

      expect(size).to.be.a('number');
      expect(size).to.not.eq(0);

      expect(url).to.be.a('string');
      expect(url.length).to.not.eq(0);

      expect(lastModified).to.be.a('number');

      expect(tags).to.be.an('object');
      expect(tags.name).to.eq(file.name);
    });
  });

  it('fetchJars', () => {
    const file = generateFile();
    cy.createJar(file.name, file.group)
      .fetchFiles(file.group)
      .then(response => {
        const {
          data: { isSuccess, result },
        } = response;

        expect(isSuccess).to.eq(true);

        expect(result).to.be.a('array');
        expect(result.length).to.eq(1);

        const fileInfo = result[0];

        expect(fileInfo).to.include.keys('name', 'group');
        expect(fileInfo.name).to.be.a('string');
        expect(fileInfo.group).to.be.a('string');
        expect(fileInfo.tags.name).to.eq(fileInfo.name);
      });
  });

  it('fetchJar', () => {
    const file = generateFile();
    cy.createJar(file.name, file.group)
      .fetchFile(file)
      .then(response => {
        const {
          data: { isSuccess, result },
        } = response;
        const { group, name, size, url, lastModified, tags } = result;

        expect(isSuccess).to.eq(true);

        expect(group).to.be.a('string');
        expect(group).to.eq(file.group);

        expect(name).to.be.a('string');
        expect(name).to.eq(file.name);

        expect(size).to.be.a('number');
        expect(size).to.not.eq(0);

        expect(url).to.be.a('string');
        expect(url.length).to.not.eq(0);

        expect(lastModified).to.be.a('number');

        expect(tags).to.be.an('object');
        expect(tags.name).to.eq(file.name);
      });
  });

  it('updateJar - tags', () => {
    const file = generateFile();

    const newFile = Object.assign({}, file, { tags: { tag: 'aaa' } });

    cy.createJar(file.name, file.group)
      .updateFile(newFile)
      .then(response => {
        const {
          data: { isSuccess, result },
        } = response;
        const { group, name, size, url, lastModified, tags } = result;

        expect(isSuccess).to.eq(true);

        expect(group).to.be.a('string');
        expect(group).to.eq(file.group);

        expect(name).to.be.a('string');
        expect(name).to.eq(file.name);

        expect(size).to.be.a('number');
        expect(size).to.not.eq(0);

        expect(url).to.be.a('string');
        expect(url.length).to.not.eq(0);

        expect(lastModified).to.be.a('number');

        expect(tags).to.be.an('object');
        expect(tags.tag).to.eq('aaa');
      });
  });

  it('deleteJar', () => {
    const file = generateFile();
    cy.createJar(file.name, file.group).deleteFile(file);

    cy.fetchFiles(file.group).then(response => {
      const {
        data: { isSuccess, result },
      } = response;

      expect(isSuccess).to.eq(true);

      expect(result.length).to.eq(0);
    });
  });
});
