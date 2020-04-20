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

import { deleteAllServices } from '../utils';
import * as fileApi from '../../src/api/fileApi';
import { hashByGroupAndName } from '../../src/utils/sha';

// The layout has been changed and so the test fails.
describe.skip('FileTable of Workspace Settings', () => {
  beforeEach(async () => await deleteAllServices());

  it('file operations in workspace should be worked normally', () => {
    // create workspace
    cy.createWorkspace({ withPlugin: false });

    // we use the default workspace name and group for file tags
    const workspaceKey = {
      name: 'workspace1',
      group: 'workspace',
    };
    const fileGroup = hashByGroupAndName(workspaceKey.group, workspaceKey.name);

    // upload files
    const source = {
      fixturePath: 'plugin',
      // we use an existing file to simulate upload jar
      name: 'ohara-it-source.jar',
      group: fileGroup,
      tags: { parentKey: workspaceKey },
    };

    const sink = {
      fixturePath: 'plugin',
      // we use an existing file to simulate upload jar
      name: 'ohara-it-sink.jar',
      group: fileGroup,
      tags: { parentKey: workspaceKey },
    };

    const stream = {
      fixturePath: 'stream',
      name: 'ohara-it-stream.jar',
      group: fileGroup,
      tags: { parentKey: workspaceKey },
    };

    const files = [source, sink, stream];

    // simulate the behavior of uploading file by actually calling API
    files.forEach(file => {
      cy.createJar(file).then(params => fileApi.create(params));
    });

    cy.contains('button', /workspace/i)
      .should('exist')
      .click();

    cy.findByText(/^files$/i)
      .should('exist')
      .click();

    cy.findByText(source.name).should('exist');
    cy.findByText(sink.name).should('exist');
    cy.findByText(stream.name).should('exist');

    // check the source file could be removed
    cy.findByText(source.name)
      .siblings('td')
      .last()
      .find('button')
      .click();
    cy.findAllByText(/^delete$/i)
      .filter(':visible')
      .click();
    // confirm dialog
    cy.findByTestId('confirm-button-DELETE').click();

    cy.findByText(source.name).should('not.exist');

    //filter
    cy.findAllByPlaceholderText('Search')
      .filter(':visible')
      .type(stream.name);
    cy.findByText(stream.name).should('exist');
    cy.findByText(sink.name).should('not.exist');

    cy.findAllByPlaceholderText('Search')
      .filter(':visible')
      .clear()
      .type('fake');
    cy.findByText(stream.name).should('not.exist');
    cy.findByText(sink.name).should('not.exist');
  });
});
