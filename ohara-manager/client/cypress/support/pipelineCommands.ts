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

import '@testing-library/cypress/add-commands';
import { capitalize } from 'lodash';

import { KIND, CELL_TYPE, CELL_STATUS } from '../../src/const';
import { hashByGroupAndName } from '../../src/utils/sha';
import { SETTING_SECTION, CELL_ACTION } from './customCommands';
import { ElementParameters } from './customCommands';

Cypress.Commands.add('createPipeline', (name = 'pipeline1') => {
  cy.log(`Creating pipeline: ${name}`);
  cy.get('.new-pipeline-button').click();
  cy.findByTestId('new-pipeline-dialog').find('input').type(name);
  cy.findByText('ADD').click();
});

Cypress.Commands.add('startPipeline', (name) => {
  cy.log(`Starting ${name}`);
  cy.get('#pipeline-list').findByText(name).click();

  cy.findByTestId('pipeline-controls-button').should('exist').click();

  cy.findByTestId('pipeline-controls-dropdown').within(() => {
    cy.findByText('Start all components').then(($el) => {
      if (!$el.hasClass('Mui-disabled')) {
        cy.wrap($el).click();
      }
    });
  });
});

Cypress.Commands.add('stopPipeline', (name) => {
  cy.log(`Stopping ${name}`);

  cy.get('#pipeline-list').findByText(name).click();

  cy.get('#paper').then(($paper) => {
    // Topics are not included in the comparison as they will always be running in the UI
    const noneTopicEls = $paper.find('.paper-element').not('.topic');
    const runningElements = noneTopicEls.find('.running');

    // If there's no running elements, no need to stop the pipeline
    if (noneTopicEls.length === 0 || runningElements.length === 0) return;

    cy.get('.pipeline-controls').find('button').click();
    cy.findByText('Stop all components').click();

    cy.wrap(noneTopicEls).should(($els) => {
      // Ensure all els are not running
      $els.each((_, el) => {
        expect(Cypress.$(el).find('.icon')).not.to.have.class(
          CELL_STATUS.running,
        );
      });
    });
  });
});

Cypress.Commands.add('deletePipeline', (name) => {
  cy.log(`Deleting ${name}`);

  cy.get('#pipeline-list').findByText(name).click();

  cy.get('.pipeline-controls').find('button').click();
  cy.findByText('Delete this pipeline').click();
  cy.findByText('DELETE').filter(':visible').click();

  cy.findByText(name).should('not.exist');
});

// Delete all pipelines under current workspace
Cypress.Commands.add('deleteAndStopAllPipelines', () => {
  cy.log(`Deleting all pipelines`);

  cy.get('#pipeline-list').then(($list) => {
    if ($list.find('> li').length === 0) return;

    cy.get('#pipeline-list > li').each(($el) => {
      const pipelineName = $el.text();
      cy.stopPipeline(pipelineName); // Stop all services before deleting
      cy.deletePipeline(pipelineName);
    });

    cy.get('#pipeline-list > li').should('have.length', 0);
  });
});

// Drag & Drop
Cypress.Commands.add(
  'dragAndDrop',
  { prevSubject: true },
  (subject: HTMLElement, shiftX: number, shiftY: number) => {
    cy.wrap(subject)
      // using the top-left position to trigger the event
      // since we calculate the moving event by rect.left and rect.top
      .trigger('mousedown', 'topLeft', { timeout: 1000, which: 1 });
    // we only get one "flying element" at one time
    // it's ok to find by testid
    cy.findByTestId('flying-element').then((element) => {
      cy.wrap(element)
        .trigger('mousemove', 'topLeft', {
          timeout: 1000,
          pageX: shiftX,
          pageY: shiftY,
          force: true,
        })
        .trigger('mouseup', 'topLeft', { timeout: 1000, force: true });
    });
  },
);

Cypress.Commands.add('addElement', ({ name, kind, className }) => {
  cy.log(
    `add element: ${name} of ${kind}` + className
      ? `with className ${className}`
      : '',
  );

  // Drag and drop an element only works on Paper, so we need to avoid dropping
  // thing in other elements
  const initialX = 600; // Toolbox + navigator + appBar: 272 + 240 + 64 px
  const initialY = 100; // Toolbar 72px height with 38px y axis offset
  const shiftWidth = 350;
  const shiftHeight = 110;

  cy.get('#paper').then(($paper) => {
    cy.log(
      'calculate the size of elements(source, sink, stream, topic) in pipeline',
    );

    const size = $paper.find('.paper-element').length;

    // Open Toolbox panel
    cy.findByText(capitalize(kind)).should('exist').click();

    // re-render the cell position to maximize the available space
    // the view of cells will be a [n, 2] matrix
    const x = size % 2 === 0 ? initialX : initialX + shiftWidth;
    const y = initialY + ~~(size / 2) * shiftHeight;
    cy.log(`Element position: ${x}, ${y}`);

    // wait a little time for the toolbox list rendered
    cy.wait(2000);

    if (kind === KIND.source || kind === KIND.sink) {
      const displayName = className.split('.').pop();

      cy.findByTestId('toolbox-draggable')
        .within(() => {
          cy.findByText(displayName)
            .should('exist')
            .and('have.class', 'display-name')
            .parent('.item')
            .should('have.attr', 'data-testid')
            .then((testId) => cy.get(`g[model-id="${testId}"]`))
            .should('exist');
        })
        .dragAndDrop(x, y);

      // type the name and add
      cy.findByLabelText(`${capitalize(kind)} name`, { exact: false }).type(
        name,
      );
      cy.findAllByText('ADD').filter(':visible').click();
    } else if (kind === KIND.topic) {
      const isSharedTopic = !name.startsWith('T');
      if (isSharedTopic) {
        cy.findByText(name).should('exist');

        let topics: string[] = [];

        $paper.find('#topic-list .display-name').each((_, element) => {
          if (element.textContent) {
            if (element.textContent === 'Pipeline Only')
              // make sure the "pipeline only" topic is in first order
              topics.push('_private');
            topics.push(element.textContent);
          }
        });

        cy.findByTestId('toolbox-draggable')
          .within(() => {
            cy.findByText(name)
              .should('exist')
              .and('have.class', 'display-name')
              .parent('.item')
              .should('have.attr', 'data-testid')
              .then((testId) => cy.get(`g[model-id="${testId}"]`))
              .should('exist');
          })
          .dragAndDrop(x, y);
      } else {
        // create a pipeline-only topic
        cy.findByTestId('toolbox-draggable')
          .find(`g[data-type="${CELL_TYPE.ELEMENT}"]:visible`)
          // the only "draggable" cell is pipeline-only topic
          .first()
          .dragAndDrop(x, y);
      }
    } else if (kind === KIND.stream) {
      cy.findByTestId('toolbox-draggable')
        .find(`g[data-type="${CELL_TYPE.ELEMENT}"]:visible`)
        // we only got 1 class for the uploaded stream jar
        // it's ok to assert the first element is the "stream class"
        .eq(0)
        .dragAndDrop(x, y);

      // type the name and add
      cy.findByLabelText(`${capitalize(kind)} name`, { exact: false }).type(
        name,
      );
      cy.findAllByText('ADD').filter(':visible').click();
    }

    // wait for the cell added
    cy.get('#outline').findByText(name).should('exist');

    // If it's a topic, we need to make sure it's started and in 'running' state
    // this is because 'pending' topic cannot be operated
    cy.get('#paper').then(($paper) => {
      if (kind === KIND.topic) {
        cy.wrap($paper)
          .findByText(name)
          .prev('svg')
          .find(`.${CELL_STATUS.running}`);
      }
    });

    // Collapse this panel
    cy.findByText(capitalize(kind)).click();
    cy.end();
  });
});

Cypress.Commands.add('addElements', (elements) => {
  cy.log('Adding multiple elements into Paper');

  elements.forEach((element: ElementParameters) => {
    cy.addElement(element);
  });
});

Cypress.Commands.add('createConnections', (elementNames) => {
  cy.log('Creating connection for multiple elements');

  elementNames.forEach((elementName: string, i: number) => {
    const nextElementName = elementNames[++i];

    // Don't create a connection from last element
    if (elementNames.length === i) return;

    cy.log(`Connecting: ${elementName} -> ${nextElementName}`);

    // Action
    cy.getCell(elementName).trigger('mouseover');
    cy.cellAction(elementName, CELL_ACTION.link).click();

    // Create the link: currentElement -> nextElement
    cy.getCell(nextElementName).click();
  });
});

Cypress.Commands.add('removeElement', (name) => {
  cy.log(`Removing an element: ${name}`);

  cy.getCell(name).trigger('mouseover');
  cy.cellAction(name, CELL_ACTION.remove).click();
  cy.findByTestId('delete-dialog').findByText('DELETE').click();

  cy.get('#paper').findByText(name).should('not.exist');

  // Ensure API requests are finished
  cy.wait(2000);
});

Cypress.Commands.add('getCell', (name) => {
  // open the cell menu
  cy.get('#paper').within(() => {
    cy.findByText(name)
      .should('exist')
      .parents(
        name.startsWith('topic') || name.startsWith('T')
          ? '.topic'
          : name.startsWith('stream')
          ? '.stream'
          : '.connector',
      )
      .then(($el) => {
        const testId = $el.attr('data-testid');
        return cy.get(`g[model-id="${testId}"]`);
      });
  });
});

Cypress.Commands.add('cellAction', (name, action: CELL_ACTION) => {
  // open the cell menu
  cy.get('#paper').within(() => {
    cy.findAllByText(name)
      .filter(':visible')
      .should('exist')
      .parents(
        name.startsWith('topic') || name.startsWith('T')
          ? '.topic'
          : name.startsWith('stream')
          ? '.stream'
          : '.connector',
      )
      .first()
      .within(() => cy.get(`button.${action.toString()}:visible`));
  });
});

Cypress.Commands.add('uploadStreamJar', () => {
  cy.log('Uploading stream jar');
  cy.switchSettingSection(SETTING_SECTION.stream);

  // click upload plugins
  cy.findAllByTitle('Add File').first().click();
  cy.findAllByTitle('Upload File').filter(':visible');

  // upload the file by custom command "createJar"
  const workspaceKey = {
    name: 'workspace1',
    group: 'workspace',
  };

  const fileGroup = hashByGroupAndName(workspaceKey.group, workspaceKey.name);
  const source = {
    fixturePath: 'jars',
    name: 'ohara-it-stream.jar',
    group: fileGroup,
  };

  cy.findByText('Select file')
    .parent('div')
    .siblings('div')
    .first()
    .find('input[type="file"]')
    .then((element) => {
      cy.createJar(source).then((params) => {
        (element[0] as HTMLInputElement).files = params.fileList;
        cy.wrap(element).trigger('change', { force: true });
      });
    });

  // after upload file, click the upload file again
  cy.wait(1000);
  cy.findAllByTitle('Upload File').first().click();

  // select the uploaded file
  cy.findByText('ohara-it-stream.jar')
    .siblings('td')
    .eq(0)
    .find('input')
    .check();

  // click save button
  cy.findAllByText('SAVE').filter(':visible').click();

  cy.findByTestId('workspace-settings-dialog-close-button').click({
    force: true,
  });
});
