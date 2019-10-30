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

import { isEmpty } from 'lodash';

import * as nodeApi from '../../src/api/nodeApi';
import * as fileApi from '../../src/api/fileApi';

// Node API
Cypress.Commands.add('fetchNodes', () => nodeApi.fetchNodes());
Cypress.Commands.add('fetchNode', params => nodeApi.fetchNode(params));
Cypress.Commands.add('createNode', params => nodeApi.createNode(params));
Cypress.Commands.add('updateNode', params => nodeApi.updateNode(params));
Cypress.Commands.add('deleteNode', params => nodeApi.deleteNode(params));

// File API
Cypress.Commands.add('fetchFiles', group => fileApi.fetchFiles(group));
Cypress.Commands.add('fetchFile', params => fileApi.fetchFile(params));
Cypress.Commands.add('uploadFile', params => fileApi.uploadFile(params));
Cypress.Commands.add('updateFile', params => fileApi.updateFile(params));
Cypress.Commands.add('deleteFile', params => fileApi.deleteFile(params));

// Utility commands
Cypress.Commands.add('createJar', (jarName, jarGroup) => {
  cy.fixture(`plugin/${jarName}`, 'base64')
    .then(Cypress.Blob.base64StringToBlob)
    .then(blob => {
      const type = 'application/java-archive';
      const testFile = new File([blob], jarName, { type });
      const dataTransfer = new DataTransfer();
      dataTransfer.items.add(testFile);
      blob = dataTransfer.files;
      const params = {
        file: blob[0],
        group: jarGroup,
        tags: {
          name: jarName,
        },
      };
      return cy.uploadFile(params);
    });
});

Cypress.Commands.add('deleteAllServices', () => {
  // delete all nodes
  cy.fetchNodes().then(response => {
    const { result: nodes } = response.data;
    if (!isEmpty(nodes)) {
      nodes
        // since there may have other nodes data exist (by configurator),
        // we should remove the generate nodes by us only.
        // TODO: remove this line after we handle the service part
        .filter(node => node.hostname.startsWith('node'))
        .forEach(node => cy.deleteNode(node));
    }
  });
  // delete all files
  cy.fetchFiles().then(response => {
    const { result: fileInfos } = response.data;
    if (!isEmpty(fileInfos)) {
      fileInfos.forEach(fileInfo => cy.deleteFile(fileInfo));
    }
  });
});
