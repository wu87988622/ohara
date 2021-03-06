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

import * as generate from '../../../src/utils/generate';
import { KIND } from '../../../src/const';
import { NodeRequest } from '../../../src/api/apiInterface/nodeInterface';
import { fetchServiceInfo } from '../../utils';
import { CELL_ACTION } from '../../support/customCommands';
import { ElementParameters } from '../../support/customCommands';
import {
  Permission,
  Type,
} from '../../../src/api/apiInterface/definitionInterface';
import { SOURCE, SINK } from '../../../src/api/apiInterface/connectorInterface';

const node: NodeRequest = {
  hostname: generate.serviceName(),
  port: generate.port(),
  user: generate.userName(),
  password: generate.password(),
};

/* eslint-disable @typescript-eslint/no-unused-expressions */
describe('Property dialog', () => {
  before(() => {
    cy.deleteAllServices();
    cy.createWorkspace({ node });
    cy.uploadStreamJar();
  });

  beforeEach(() => {
    cy.deleteAndStopAllPipelines();
    cy.createPipeline();
  });

  context('UI', () => {
    it('should render Property view UI', () => {
      // Create a Perf source
      const sourceName = generate.serviceName({ prefix: 'source' });
      cy.addElement({
        name: sourceName,
        kind: KIND.source,
        className: SOURCE.perf,
      });

      // Open dialog
      cy.getCell(sourceName).trigger('mouseover');
      cy.cellAction(sourceName, CELL_ACTION.config).click();

      cy.findByTestId('property-dialog').within(() => {
        // Title and close button
        cy.findByText(`Edit the property of ${sourceName}`).should('exist');
        cy.findByTestId('close-button').should('exist');

        // Panels
        cy.findByTestId('sidebar').within(() => {
          // Common section should be expanded
          cy.findByText('Common')
            .should('exist')
            .parent()
            .should('have.class', 'Mui-expanded');

          // Only one panel is expanded by default
          cy.get('.MuiExpansionPanel-root.Mui-expanded').should(
            'have.length',
            1,
          );

          // These two panels should also be there
          cy.findByText('Core').should('exist');
          cy.findByText('Meta').should('exist');
        });

        cy.findByTestId('speed-dialog').should('exist');

        // The button is there and not being disabled
        cy.findByText('SAVE CHANGES').should('exist').and('not.be.disabled');

        // Close the dialog
        cy.findByTestId('close-button').click();
      });
    });

    it('should render the form with definition APIs', () => {
      // Create a Perf source
      const sourceName = generate.serviceName({ prefix: 'source' });
      cy.addElement({
        name: sourceName,
        kind: KIND.source,
        className: SOURCE.perf,
      });

      // Open dialog
      cy.getCell(sourceName).trigger('mouseover');
      cy.cellAction(sourceName, CELL_ACTION.config).click();

      cy.findByTestId('property-dialog').then(async () => {
        const workerDefs = await fetchServiceInfo(KIND.source, {
          group: 'worker',
          name: 'workspace1',
        });

        const perfDefs = workerDefs.classInfos.find(
          (info) => info.className === SOURCE.perf,
        );

        // internal fields are hidden from UI
        const renderedDefs = perfDefs?.settingDefinitions
          .filter((def) => !def.internal)
          .filter((def) => def.key !== 'group' && def.key !== 'tags');

        renderedDefs?.forEach((def) => {
          cy.findByTestId('definition-content').within(() => {
            // All fields should render the document of its definition
            cy.get('.MuiFormHelperText-root')
              .contains(def.documentation)
              .should('exist');

            // A display should also render unless it's a Table
            if (def.valueType !== Type.TABLE) {
              cy.get('.MuiFormLabel-root')
                .contains(def.displayName)
                .should('exist');
            } else {
              cy.findByText(def.displayName).should('exist');
            }

            if (def.defaultValue) {
              const expectedValue =
                def.valueType === Type.DURATION
                  ? getDuration(def.defaultValue)
                  : def.defaultValue;

              // Default value should be render too
              cy.get(`input[name="${def.key.replace(/\./g, '__')}"]`)
                .invoke('val')
                .should('equal', String(expectedValue));

              // Should disable the field if it's a read only or create only field
              if (
                def.permission === Permission.READ_ONLY ||
                def.permission === Permission.CREATE_ONLY
              ) {
                cy.get(`input[name="${def.key.replace(/\./g, '__')}"]`).should(
                  'have.class',
                  'Mui-disabled',
                );
              }
            }
          });
        });

        // Close the dialog
        cy.findByTestId('property-dialog').findByTestId('close-button').click();
      });
    });

    it('should be able to expand all definition panels', () => {
      // Create a Perf source
      const sourceName = generate.serviceName({ prefix: 'source' });
      cy.addElement({
        name: sourceName,
        kind: KIND.source,
        className: SOURCE.perf,
      });

      // Open dialog
      cy.getCell(sourceName).trigger('mouseover');
      cy.cellAction(sourceName, CELL_ACTION.config).click();

      cy.findByTestId('property-dialog').within(() => {
        cy.findByTestId('sidebar').within(() => {
          // Common section should be expanded
          cy.findByText('Common')
            .should('exist')
            .parent()
            .should('have.class', 'Mui-expanded');

          // Only one panel is expanded by default
          cy.get('.MuiExpansionPanel-root.Mui-expanded').should(
            'have.length',
            1,
          );

          // Open core panel
          cy.findByText('Core')
            .click()
            .parents('.MuiExpansionPanel-root')
            .find('.MuiExpansionPanelDetails-root')
            .should('exist');

          // Only one panel is expanded
          cy.get('.MuiExpansionPanel-root.Mui-expanded').should(
            'have.length',
            1,
          );

          // Open meta panel
          cy.findByText('Meta')
            .click()
            .parents('.MuiExpansionPanel-root')
            .find('.MuiExpansionPanelDetails-root')
            .should('exist');

          cy.get('.MuiExpansionPanel-root.Mui-expanded').should(
            'have.length',
            1,
          );
        });

        // Close the dialog
        cy.findByTestId('close-button').click();
      });
    });

    it('should be able to add new schema', () => {
      // Create a Perf source
      const sourceName = generate.serviceName({ prefix: 'source' });
      cy.addElement({
        name: sourceName,
        kind: KIND.source,
        className: SOURCE.perf,
      });

      // Open dialog
      cy.getCell(sourceName).trigger('mouseover');
      cy.cellAction(sourceName, CELL_ACTION.config).click();

      cy.findByTestId('definition-table').within(() => {
        // Ensure the table is ready
        cy.findByText('Schema').should('exist');

        // No records for now
        cy.findByText('No records to display').should('exist');

        // Add a new schema
        cy.findByTitle('Add').should('exist').click();

        cy.findByPlaceholderText('order').type('1');
        cy.findByPlaceholderText('name').type('john');
        cy.findByPlaceholderText('newName').type('johndoe');
        cy.get('div[aria-label="dataType"]').click();
      });

      cy.get('.MuiMenu-paper:visible').findByText('INT').click();

      cy.findByTestId('definition-table').within(() => {
        // Save it
        cy.findByTitle('Save').click();

        // Assert the data are present
        cy.findByText('No records to display').should('not.exist');
        cy.findByText('1').should('exist');
        cy.findByText('INT').should('exist');
        cy.findByText('john').should('exist');
        cy.findByText('johndoe').should('exist');
      });

      // Close the dialog
      cy.findByTestId('property-dialog').findByTestId('close-button').click();
    });

    it('should be able to delete a schema', () => {
      // Create a Perf source
      const sourceName = generate.serviceName({ prefix: 'source' });
      cy.addElement({
        name: sourceName,
        kind: KIND.source,
        className: SOURCE.perf,
      });

      // Open dialog
      cy.getCell(sourceName).trigger('mouseover');
      cy.cellAction(sourceName, CELL_ACTION.config).click();

      cy.findByTestId('definition-table').within(() => {
        // Ensure the table is ready
        cy.findByText('Schema').should('exist');
        cy.findByText('No records to display').should('exist');

        // Add a new schema
        cy.findByTitle('Add').should('exist').click();

        cy.findByPlaceholderText('order').type('1');
        cy.findByPlaceholderText('name').type('john');
        cy.findByPlaceholderText('newName').type('johndoe');
        cy.get('div[aria-label="dataType"]').click();
      });

      cy.get('.MuiMenu-paper:visible').findByText('INT').click();

      cy.findByTestId('definition-table').within(() => {
        // Save it
        cy.findByTitle('Save').click();

        // Assert the data are present
        cy.findByText('No records to display').should('not.exist');

        // Delete the data
        cy.findByTitle('Delete').click();
        cy.findByText('Are you sure you want to delete this row?');
        cy.findByTitle('Save').click();

        // It should no longer present
        cy.findByText('No records to display').should('exist');
      });

      // Close the dialog
      cy.findByTestId('property-dialog').findByTestId('close-button').click();
    });

    it('should be able to update a schema', () => {
      // Create a Perf source
      const sourceName = generate.serviceName({ prefix: 'source' });
      cy.addElement({
        name: sourceName,
        kind: KIND.source,
        className: SOURCE.perf,
      });

      // Open dialog
      cy.getCell(sourceName).trigger('mouseover');
      cy.cellAction(sourceName, CELL_ACTION.config).click();

      cy.findByTestId('definition-table').within(() => {
        // Ensure the table is ready
        cy.findByText('Schema').should('exist');
        cy.findByText('No records to display').should('exist');

        // Add a new schema
        cy.findByTitle('Add').should('exist').click();

        cy.findByPlaceholderText('order').type('1');
        cy.findByPlaceholderText('name').type('john');
        cy.findByPlaceholderText('newName').type('johndoe');
        cy.get('div[aria-label="dataType"]').click();
      });

      cy.get('.MuiMenu-paper:visible').findByText('INT').click();

      cy.findByTestId('definition-table').within(() => {
        // Save it
        cy.findByTitle('Save').click();

        // Assert the data are present
        cy.findByText('No records to display').should('not.exist');
        cy.findByText('1').should('exist');
        cy.findByText('INT').should('exist');
        cy.findByText('john').should('exist');
        cy.findByText('johndoe').should('exist');

        // Edit
        cy.findByTitle('Edit').click();

        cy.findByPlaceholderText('order').clear().type('20');
        cy.findByPlaceholderText('name').clear().type('jane');
        cy.findByPlaceholderText('newName').clear().type('janedoe');
        cy.get('div[aria-label="dataType"]').click();
      });

      cy.get('.MuiMenu-paper:visible').findByText('STRING').click();

      cy.findByTestId('definition-table').within(() => {
        cy.findByTitle('Save').click();

        cy.findByText('No records to display').should('not.exist');
        cy.findByText('20').should('exist');
        cy.findByText('STRING').should('exist');
        cy.findByText('jane').should('exist');
        cy.findByText('janedoe').should('exist');
      });

      // Close the dialog
      cy.findByTestId('property-dialog').findByTestId('close-button').click();
    });
  });

  context('Dialog interaction', () => {
    it('should prevent users from saving the form when necessary fields are not filled', () => {
      // Create a FTP source, we need a FTP source here since it has many "required" fields and so can
      //  be used for our testing spec
      const sourceName = generate.serviceName({ prefix: 'source' });
      cy.addElement({
        name: sourceName,
        kind: KIND.source,
        className: SOURCE.ftp,
      });

      // Open dialog
      cy.getCell(sourceName).trigger('mouseover');
      cy.cellAction(sourceName, CELL_ACTION.config).click();

      cy.findByTestId('property-dialog').within(() => {
        // The button is there and not being disabled
        cy.findByText('SAVE CHANGES').should('exist').click();

        // It should fail to close the dialog and so the title is still there
        cy.findByText(`Edit the property of ${sourceName}`)
          .should('exist')
          .and('be.visible');

        // Should have at least one warning text in the form
        cy.findAllByText('This is a required field').should(
          'have.length.gt',
          1,
        );

        // The test is done, close the dialog so it won't interfere our next test
        cy.findByTestId('close-button').click();
      });
    });

    it('should be able to open the dialog', () => {
      const elements: ElementParameters[] = [
        {
          name: generate.serviceName({ prefix: 'source' }),
          kind: KIND.source,
          className: SOURCE.jdbc,
        },
        {
          name: generate.serviceName({ prefix: 'source' }),
          kind: KIND.source,
          className: SOURCE.shabondi,
        },
        {
          name: generate.serviceName({ prefix: 'sink' }),
          kind: KIND.sink,
          className: SINK.hdfs,
        },
        {
          name: generate.serviceName({ prefix: 'sink' }),
          kind: KIND.sink,
          className: SINK.shabondi,
        },
        {
          name: generate.serviceName({ prefix: 'stream' }),
          kind: KIND.stream,
          className: KIND.stream,
        },
      ];

      elements.forEach(({ name, ...rest }) => {
        cy.addElement({ name, ...rest });

        cy.getCell(name).trigger('mouseover');
        cy.cellAction(name, CELL_ACTION.config).click();

        // Should have the dialog title
        cy.findByText(`Edit the property of ${name}`).should('exist');

        // Close the dialog
        cy.findByTestId('property-dialog').findByTestId('close-button').click();
      });
    });

    it('should save and persist the changes', () => {
      const inputValue = String(generate.number());

      // Create a Perf source
      const sourceName = generate.serviceName({ prefix: 'source' });
      cy.addElement({
        name: sourceName,
        kind: KIND.source,
        className: SOURCE.perf,
      });

      // Open dialog
      cy.getCell(sourceName).trigger('mouseover');
      cy.cellAction(sourceName, CELL_ACTION.config).click();

      // TODO: #5377 associate field label and input and so we can use `findByLabelText` in our "definition base" forms
      cy.get('input[name="perf__batch"]') // This field is supposed to be in Perf source connector
        .should('exist')
        .clear()
        // Due to a bug (#4247) in our UI, we cannot reset the value of a number input to "0", and
        // so we need to manually delete that extra "0" in the value or we will get "200" instead of "20"
        .type(`${inputValue}{rightarrow}{backspace}`)
        .blur();

      // Assert if the value exists and save
      cy.findByDisplayValue(inputValue).should('exist');
      cy.findByText('SAVE CHANGES').click();

      // Reload and see if our data is persisted
      cy.reload();

      // Open dialog
      cy.getCell(sourceName).trigger('mouseover');
      cy.cellAction(sourceName, CELL_ACTION.config).click();

      // The value should be kept
      cy.findByDisplayValue(inputValue).should('exist');

      // Close the dialog
      cy.findByTestId('property-dialog').findByTestId('close-button').click();
    });

    it('should close the dialog by hitting escape key and clicking on backdrop', () => {
      // Create a Perf source
      const sourceName = generate.serviceName({ prefix: 'source' });
      cy.addElement({
        name: sourceName,
        kind: KIND.source,
        className: SOURCE.perf,
      });

      // Open dialog
      cy.getCell(sourceName).trigger('mouseover');
      cy.cellAction(sourceName, CELL_ACTION.config).click();

      // It's opened
      cy.findByTestId('property-dialog').should('visible');

      // Clicking on backdrop
      cy.get('.MuiBackdrop-root').click({ force: true });

      // The dialog should be closed
      cy.findByTestId('property-dialog').should('not.visible');

      // Open the dialog again
      cy.getCell(sourceName).trigger('mouseover');
      cy.cellAction(sourceName, CELL_ACTION.config).click();

      // It's opened
      cy.findByTestId('property-dialog').should('visible');

      // Use escape key to close it
      cy.get('body').trigger('keydown', { keyCode: 27 });

      // It should be closed again
      cy.findByTestId('property-dialog').should('not.visible');
    });
  });

  context('Interaction with Paper', () => {
    it('should be able to create and remove a link via editing topic field', () => {
      // Create a perf source and pipeline-only topic
      const sourceName = generate.serviceName({ prefix: 'source' });
      const pipelineOnlyTopicName = 'T1';

      cy.addElements([
        {
          name: sourceName,
          kind: KIND.source,
          className: SOURCE.perf,
        },
        {
          name: pipelineOnlyTopicName,
          kind: KIND.topic,
        },
      ]);

      // No link yet
      cy.get('#paper .joint-link').should('have.length', 0);

      // Open topics list and select the topic
      updateTopicField({
        elementName: sourceName,
        currentTopicName: 'Please select...',
        newTopicName: pipelineOnlyTopicName,
      });

      // The link should be created
      cy.get('#paper .joint-link').should('have.length', 1);

      // Open dialog again for resetting the link this time
      updateTopicField({
        elementName: sourceName,
        currentTopicName: pipelineOnlyTopicName,
        newTopicName: 'Please select...',
      });

      // The link should be removed
      cy.get('#paper .joint-link').should('have.length', 0);
    });

    it(`should only maintain one connection while updating source connector's topic field`, () => {
      const perfSourceName = generate.serviceName({ prefix: 'source' });
      const pipelineTopicName1 = 'T1';
      const pipelineTopicName2 = 'T2';

      cy.addElements([
        {
          name: perfSourceName,
          kind: KIND.source,
          className: SOURCE.perf,
        },
        {
          name: pipelineTopicName1,
          kind: KIND.topic,
        },
        {
          name: pipelineTopicName2,
          kind: KIND.topic,
        },
      ]);

      // No link yet
      cy.get('#paper .joint-link').should('have.length', 0);

      // Start with perf source
      updateTopicField({
        elementName: perfSourceName,
        currentTopicName: 'Please select...',
        newTopicName: pipelineTopicName1,
      });

      cy.get('#paper .joint-link').should('have.length', 1);

      updateTopicField({
        elementName: perfSourceName,
        currentTopicName: pipelineTopicName1,
        newTopicName: pipelineTopicName2,
      });

      cy.get('#paper .joint-link').should('have.length', 1);

      // Ensure the logic is also applied and saved in the Backend as well
      cy.reload();
      cy.get('#paper .joint-link').should('have.length', 1);
    });

    it(`Should only maintain one connection while updating shabondis source connector's topic field`, () => {
      const shabondiSourceName = generate.serviceName({ prefix: 'source' });
      const pipelineTopicName1 = 'T1';
      const pipelineTopicName2 = 'T2';

      cy.addElements([
        {
          name: shabondiSourceName,
          kind: KIND.source,
          className: SOURCE.shabondi,
        },
        {
          name: pipelineTopicName1,
          kind: KIND.topic,
        },
        {
          name: pipelineTopicName2,
          kind: KIND.topic,
        },
      ]);

      // No link yet
      cy.get('#paper .joint-link').should('have.length', 0);

      // Start with perf source
      updateTopicField({
        elementName: shabondiSourceName,
        currentTopicName: 'Please select...',
        newTopicName: pipelineTopicName1,
      });

      cy.get('#paper .joint-link').should('have.length', 1);

      updateTopicField({
        elementName: shabondiSourceName,
        currentTopicName: pipelineTopicName1,
        newTopicName: pipelineTopicName2,
      });

      cy.get('#paper .joint-link').should('have.length', 1);

      // Ensure the logic is also applied and saved in the Backend as well
      cy.reload();
      cy.get('#paper .joint-link').should('have.length', 1);
    });

    it(`Should only maintain one connection while updating shabondis sink connector's topic field`, () => {
      const shabondiSinkName = generate.serviceName({ prefix: 'source' });
      const pipelineTopicName1 = 'T1';
      const pipelineTopicName2 = 'T2';

      cy.addElements([
        {
          name: shabondiSinkName,
          kind: KIND.sink,
          className: SINK.shabondi,
        },
        {
          name: pipelineTopicName1,
          kind: KIND.topic,
        },
        {
          name: pipelineTopicName2,
          kind: KIND.topic,
        },
      ]);

      // No link yet
      cy.get('#paper .joint-link').should('have.length', 0);

      // Start with perf source
      updateTopicField({
        elementName: shabondiSinkName,
        currentTopicName: 'Please select...',
        newTopicName: pipelineTopicName1,
      });

      cy.get('#paper .joint-link').should('have.length', 1);

      updateTopicField({
        elementName: shabondiSinkName,
        currentTopicName: pipelineTopicName1,
        newTopicName: pipelineTopicName2,
      });

      cy.get('#paper .joint-link').should('have.length', 1);

      // Ensure the logic is also applied and saved in the Backend as well
      cy.reload();
      cy.get('#paper .joint-link').should('have.length', 1);
    });
  });
});

function getDuration(value: String): Number {
  return Number(value.split(' ')[0]) / 1000;
}

type updateTopicFieldParams = {
  elementName: string;
  currentTopicName: string;
  newTopicName: string;
};

function updateTopicField({
  elementName,
  currentTopicName,
  newTopicName,
}: updateTopicFieldParams): void {
  // Open dialog
  cy.getCell(elementName).trigger('mouseover');
  cy.cellAction(elementName, CELL_ACTION.config).click();

  // Open topics list and select the topic
  cy.findByTestId('definition-content').within(() => {
    cy.findByText(currentTopicName).click();
  });

  // There should only be one visible at the moment
  cy.get('.MuiPopover-root:visible').within(() => {
    cy.findByText(newTopicName).click();
  });

  // Save and close the form
  cy.findByText('SAVE CHANGES').click();
}
