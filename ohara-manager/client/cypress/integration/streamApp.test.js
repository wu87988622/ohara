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
import { makeRandomStr } from '../utils';

let pipelineParams;

describe.skip('StreamApp', () => {
  beforeEach(() => {
    pipelineParams = {
      name: makeRandomStr(),
      workerName: 'cluster',
    };
    cy.createPipeline(pipelineParams);
    cy.visit(URLS.PIPELINE);
  });

  it('adds a streamApp into pipeline graph', () => {
    cy.getByText(pipelineParams.name)
      .then($el => {
        cy.wrap($el.parent()).as('targetPipeline');

        cy.get('@targetPipeline')
          .getByTestId('edit-pipeline')
          .click();
      })
      .location('pathname')
      .should('contain', '/pipelines/edit/');

    cy.getByTestId('toolbar-streams')
      .click()
      .uploadJar(
        'input[type=file]',
        'streamApp/streamApp.jar',
        'streamApp.jar',
        'application/java-archive',
      )
      .wait(500);

    cy.getByText('Stream app successfully uploaded!')
      .should('have.length', 1)
      .getByText('streamApp.jar')
      .getByText('Add')
      .click()
      .getByText('Untitled streamApp')
      .should('be.exist');
  });

  it('edits streamApp name', () => {
    cy.getByText(pipelineParams.name)
      .then($el => {
        cy.wrap($el.parent()).as('targetPipeline');

        cy.get('@targetPipeline')
          .getByTestId('edit-pipeline')
          .click();
      })
      .getByTestId('toolbar-streams')
      .click()
      .get('input[type=file]')
      .uploadJar(
        'input[type=file]',
        'streamApp/streamApp.jar',
        'streamApp.jar',
        'application/java-archive',
      )
      .wait(500)
      .getByText('streamApp.jar')
      .click()
      .getByTestId('title-input')
      .type('{leftarrow}{leftarrow}{leftarrow}{leftarrow}')
      .type('_2{enter}')
      .getByTestId('stream-app-item')
      .find('label')
      .contains('streamApp_2.jar');
  });

  it('deletes streamApp', () => {
    cy.getByText(pipelineParams.name)
      .then($el => {
        cy.wrap($el.parent()).as('targetPipeline');

        cy.get('@targetPipeline')
          .getByTestId('edit-pipeline')
          .click();
      })
      .getByTestId('toolbar-streams')
      .click()
      .get('input[type=file]')
      .uploadJar(
        'input[type=file]',
        'streamApp/streamApp.jar',
        'streamApp.jar',
        'application/java-archive',
      )
      .wait(500)
      .getByTestId('delete-stream-app')
      .click()
      .getByText('Yes, Delete this row')
      .click()
      .wait(500)
      .queryAllByTestId('stream-app-item')
      .should('have.length', 0);
  });
});
