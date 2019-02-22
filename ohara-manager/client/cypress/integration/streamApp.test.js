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

import * as URLS from '../../src/constants/urls';

const testData = {
  cluster: 'embedded_worker_cluster',
  pipeline: {
    name: 'pl00',
  },
  jarName: 'streamApp.jar',
};

describe('StreamApp', () => {
  before(() => {
    cy.deleteAllPipelines();
    cy.insertPipeline(testData.cluster, testData.pipeline);
  });

  beforeEach(() => {
    cy.visit(URLS.PIPELINE);
  });

  it('creates a streamApp and displays in the streamApp list modal', () => {
    // open pipeline edit page
    cy.getAllByTestId('edit-pipeline')
      .should('have.length', 1)
      .click()
      .location('pathname')
      .should('contain', '/pipelines/edit/');

    // open streamApp modal and upload a jar file
    cy.getByTestId('toolbar-streams')
      .click()
      .get('input[type=file]')
      .uploadFile(testData.jarName)
      .wait(500)
      .getAllByTestId('stream-app-item')
      .should('have.length', 1);
  });

  it('updates streamApp name to "streamApp_2.jar"', () => {
    // open streamApp modal
    cy.getByTestId('edit-pipeline')
      .click()
      .getByTestId('toolbar-streams')
      .click();

    // rename to 'streamApp_2.jar'
    cy.getByText(testData.jarName)
      .click()
      .getByTestId('title-input')
      .type('{leftarrow}{leftarrow}{leftarrow}{leftarrow}')
      .type('_2{enter}')
      .getByTestId('stream-app-item')
      .find('label')
      .contains('streamApp_2.jar');
  });

  it('deletes streamApp', () => {
    // open streamApp modal
    cy.getByTestId('edit-pipeline')
      .click()
      .getByTestId('toolbar-streams')
      .click();

    // deletes the first item from stream app list
    cy.getByTestId('delete-stream-app')
      .click()
      .getByText('Yes, Delete this row')
      .click()
      .wait(500)
      .queryAllByTestId('stream-app-item')
      .should('have.length', 0);
  });
});
