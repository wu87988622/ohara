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

import { capitalize } from 'lodash';

import { deleteAllServices } from '../utils';
import { KIND, CELL_TYPES } from '../../src/const';
import * as generate from '../../src/utils/generate';
import { SOURCES, SINKS } from '../../src/api/apiInterface/connectorInterface';

const sources = Object.values(SOURCES).sort();
const sinks = Object.values(SINKS).sort();
let topics: string[] = [];

Cypress.Commands.add('addElement', (name, kind, className) => {
  cy.log(`add element: ${name} of ${kind} with className ${className}`);
  // toolbox: 272 width + navigator: 240 width + appBar: 64 width, we need to avoid covering it
  const initialX = 600;
  // the controllers tab has approximate 72 height, we need to avoid covering it
  const initialY = 100;
  const shiftWidth = 350;
  const shiftHeight = 110;

  cy.get('body').then($body => {
    let size = topics.length;
    cy.log(
      'calculate the size of elements(source, sink, stream, topic) in pipeline',
    );
    if ($body.find('div.connector').length > 0)
      size = size + $body.find('div.connector').length;

    cy.findByText(capitalize(kind))
      .should('exist')
      .click();

    // re-render the cell position to maximize the available space
    // the view of cells will be a [n, 2] matrix
    const x = size % 2 === 0 ? initialX : initialX + shiftWidth;
    const y = initialY + ~~(size / 2) * shiftHeight;
    cy.log(`element position: ${x}, ${y}`);

    // wait a little time for the toolbox list rendered
    cy.wait(2000);

    if (kind === KIND.source || kind === KIND.sink) {
      const elementIndex =
        kind === KIND.source
          ? sources.indexOf(className)
          : sinks.indexOf(className);

      cy.findByTestId('toolbox-draggable')
        .find(`g[data-type="${CELL_TYPES.ELEMENT}"]:visible`)
        // the element index to be added
        .eq(elementIndex)
        .dragAndDrop(x, y);

      // type the name and add
      cy.findByLabelText(`${capitalize(kind)} name`, { exact: false }).type(
        name,
      );
      cy.findAllByText(/^add$/i)
        .filter(':visible')
        .click();
    } else if (kind === KIND.topic) {
      topics.push(name);
      cy.log(`Available topics in this pipeline: ${topics.join(',')}`);
      if (!name.startsWith('T')) {
        // create a shared topic
        cy.findByText('Add topics')
          .siblings('button')
          .first()
          .click();

        cy.findAllByLabelText('topic name', { exact: false })
          .filter(':visible')
          .type(name);
        cy.findAllByLabelText('partitions', { exact: false })
          .filter(':visible')
          .type('1');
        cy.findAllByLabelText('replication factor', { exact: false })
          .filter(':visible')
          .type('1');
        cy.findAllByText(/^add$/i)
          .filter(':visible')
          .click();

        cy.findByText(name).should('exist');

        // wait a little time for the topic show in toolbox
        cy.wait(3000);

        cy.findByTestId('toolbox-draggable')
          .find(`g[data-type="${CELL_TYPES.ELEMENT}"]:visible`)
          // the element index to be added
          // the pipeline-only element is always first, we shift one element
          .eq(topics.sort().indexOf(name) + 1)
          .dragAndDrop(x, y);
      } else {
        // create a pipeline-only topic
        cy.findByTestId('toolbox-draggable')
          .find(`g[data-type="${CELL_TYPES.ELEMENT}"]:visible`)
          // the only "draggable" cell is pipeline-only topic
          .first()
          .dragAndDrop(x, y);
      }
    } else if (kind === KIND.stream) {
      cy.findByTestId('toolbox-draggable')
        .find(`g[data-type="${CELL_TYPES.ELEMENT}"]:visible`)
        // we only got 1 class for the uploaded stream jar
        // it's ok to assert the first element is the "stream class"
        .eq(0)
        .dragAndDrop(x, y);

      // type the name and add
      cy.findByLabelText(`${capitalize(kind)} name`, { exact: false }).type(
        name,
      );
      cy.findAllByText(/^add$/i)
        .filter(':visible')
        .click();
    }

    // wait a little time for the cell added
    cy.wait(3000);

    // close this panel
    cy.findByText(capitalize(kind)).click();
  });
});

Cypress.Commands.add('getCell', name => {
  // open the cell menu
  cy.findAllByText(name)
    .filter(':visible')
    .should('exist')
    .parents(
      name.startsWith('topic') || name.startsWith('T')
        ? 'div.topic'
        : 'div.connector',
    )
    .first()
    .then(el => {
      const testId = el[0].getAttribute('data-testid');
      return cy.get(`g[model-id="${testId}"]`);
    });
});

Cypress.Commands.add('cellAction', (name, action) => {
  // open the cell menu
  cy.findAllByText(name)
    .filter(':visible')
    .should('exist')
    .parents(
      name.startsWith('topic') || name.startsWith('T')
        ? 'div.topic'
        : 'div.connector',
    )
    .first()
    .within(() => {
      cy.get(`button.${action}:visible`);
    });
});

function openDetailDialog(topicName: string) {
  // Navigate to workspace topics page
  cy.findByText(/^workspace1/i)
    .click()
    .findByText(/^topics$/i)
    .click();

  // Grab the right topic by its name
  cy.findByTestId('workspace-settings-topics-table').within(() => {
    cy.findByText(topicName)
      .parent()
      .within(() => {
        cy.findByText(/^view$/i).click();
      });
  });
}

// TODO: will be enabled in https://github.com/oharastream/ohara/issues/4131
describe.skip('WorkspaceSettings', () => {
  before(async () => await deleteAllServices());

  it('disables the delete button if a topic is already use by a pipeline', () => {
    cy.createWorkspace({});

    // Add new pipeline
    cy.findByText(/^pipelines$/i)
      .siblings('svg')
      .first()
      .click()
      .findByText(/^add a new pipeline$/i)
      .should('exist');

    cy.findByTestId('new-pipeline-dialog')
      .find('input')
      .type('pipeline1');

    cy.findByText(/^add$/i).click();

    // force to reload the page in order to load the correct data for Toolbox
    cy.reload();

    const sharedTopicName = generate.serviceName({ prefix: 'topic' });
    cy.addElement(sharedTopicName, KIND.topic);

    openDetailDialog(sharedTopicName);

    // Assertion
    cy.findByTestId('view-topic-detail-dialog').within(() => {
      cy.findByText(/^delete/i)
        .parent('Button')
        .should('be.disabled');
    });

    // Remove the topic
    cy.visit('/');
    cy.getCell(sharedTopicName).trigger('mouseover');
    cy.cellAction(sharedTopicName, 'remove').click();
    cy.findByText(/^delete$/i).click();

    openDetailDialog(sharedTopicName);

    // Assertion
    cy.findByTestId('view-topic-detail-dialog').within(() => {
      cy.findByText(/^delete/i)
        .parent('Button')
        .should('not.be.disabled')
        .click();
    });

    // Delete the topic
    cy.findByTestId('view-topic-detail-delete-dialog').within(() => {
      cy.findByText(/^delete$/i).click();
    });

    // Assertion
    cy.findAllByText(sharedTopicName).should('have.length', 0);
  });
});
