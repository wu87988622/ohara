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
import { KIND, CELL_STATUS } from '../../src/const';
import {
  ElementParameters,
  CELL_ACTION,
  SETTING_SECTION,
} from '../support/customCommands';
import { SOURCE, SINK } from '../../src/api/apiInterface/connectorInterface';
import { hashByGroupAndName } from '../../src/utils/sha';

describe('Pipeline', () => {
  context('Create connections from scratch', () => {
    it('Perf source -> Topic -> Stream -> Topic -> Console sink', () => {
      // Prepare the environment for the test
      cy.createWorkspace({});
      cy.createPipeline();
      cy.uploadStreamJar();

      // Add elements and start the pipeline
      const perfSourceName = generate.serviceName({ prefix: 'source' });
      const consoleSinkName = generate.serviceName({ prefix: 'sink' });
      const streamName = generate.serviceName({ prefix: 'stream' });
      const pipelineOnlyTopicName1 = 'T1';
      const pipelineOnlyTopicName2 = 'T2';

      const elements: ElementParameters[] = [
        {
          name: perfSourceName,
          kind: KIND.source,
          className: SOURCE.perf,
        },
        {
          name: pipelineOnlyTopicName1,
          kind: KIND.topic,
        },
        {
          name: streamName,
          kind: KIND.stream,
        },
        {
          name: pipelineOnlyTopicName2,
          kind: KIND.topic,
        },
        {
          name: consoleSinkName,
          kind: KIND.sink,
          className: SINK.console,
        },
      ];

      cy.addElements(elements);
      cy.createConnections(elements.map((element) => element.name));

      // Configure stream element
      cy.getCell(streamName).trigger('mouseover');
      cy.cellAction(streamName, CELL_ACTION.config).click();

      cy.findByLabelText('Node name list').click();
      cy.findByText(Cypress.env('nodeHost'))
        .should('exist')
        .find('input[type="checkbox"]')
        .check()
        .should('be.checked');

      cy.findByText('SAVE CHANGES').click();

      // Start the pipeline
      cy.startPipeline('pipeline1');

      // Ensure we have the running elements we're expecting
      cy.get('#paper .paper-element')
        .not('.topic')
        .as('elements')
        .should('have.length', 3)
        .find(`.${CELL_STATUS.running}`)
        .should('have.length', 3);

      // Toggle metrics switch
      cy.findByTestId('metrics-switch')
        .click()
        .should('have.class', 'Mui-checked');

      // Metrics should be there
      cy.get('@elements')
        .find('.metrics')
        .then(($metricsEls) => {
          expect($metricsEls.length).to.eq(3);

          // Loading text should not be displayed
          cy.wrap($metricsEls)
            .findAllByText('Loading metrics')
            .should('have.length', 0);

          // No metrics data available should not be displayed
          cy.wrap($metricsEls)
            .findAllByText('No metrics data available')
            .should('have.length', 0);

          // Should have metrics value that are coming from Backend APIs
          cy.wrap($metricsEls)
            .find('.metrics-value', { timeout: 40000 })
            .should(($value) => {
              expect(Number($value.text())).to.be.greaterThan(0);
            });
        });
    });
  });

  context('Test all connectors', () => {
    const workspaceKey = {
      name: 'workspace1',
      group: 'workspace',
    };
    const fileGroup = hashByGroupAndName(workspaceKey.group, workspaceKey.name);

    // create files
    const source = {
      fixturePath: 'jars',
      name: 'ohara-it-source.jar',
      group: fileGroup,
    };
    const sink = {
      fixturePath: 'jars',
      name: 'ohara-it-sink.jar',
      group: fileGroup,
    };
    const files = [source, sink];

    it('should able to open the dialog', () => {
      cy.deleteAllServices();
      cy.createWorkspace({});
      cy.uploadStreamJar();
      cy.createPipeline();

      cy.switchSettingSection(SETTING_SECTION.files);

      cy.get('div.section-page-content').within(() => {
        // upload the files by custom command "createJar"
        files.forEach((file) => {
          cy.get('input[type="file"]').then((element) => {
            cy.createJar(file).then((params) => {
              (element[0] as HTMLInputElement).files = params.fileList;
              cy.wrap(element).trigger('change', { force: true });
            });
          });
        });

        // after upload file, click the upload file again
        cy.wait(1000);
        cy.findByTitle('Upload File').click();
      });

      cy.switchSettingSection(
        SETTING_SECTION.worker,
        'Worker plugins and shared jars',
      );

      cy.get('div.plugins').findByTitle('Add File').click();

      cy.findByText('ohara-it-sink.jar')
        .siblings('td')
        .eq(0)
        .find('input')
        .check();
      cy.findByText('ohara-it-source.jar')
        .siblings('td')
        .eq(0)
        .find('input')
        .check();
      cy.findByText('SAVE').click();

      cy.switchSettingSection(
        SETTING_SECTION.dangerZone,
        'Restart this worker',
      );
      cy.findAllByRole('dialog')
        .filter(':visible')
        .should('have.length', 1)
        .within(() => {
          cy.findByText('RESTART').click();
        });

      cy.findByText('CLOSE').parent('button').should('be.enabled').click();

      // close the snackbar
      cy.findByTestId('snackbar').find('button:visible').click();

      // close the settings dialog
      cy.findByTestId('workspace-settings-dialog-close-button')
        .should('be.visible')
        .click();

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
        {
          name: generate.serviceName({
            prefix: 'jarsource',
          }),
          kind: KIND.source,
          className:
            'oharastream.ohara.it.connector.IncludeAllTypesSourceConnector',
        },
        {
          name: generate.serviceName({
            prefix: 'jarsink',
          }),
          kind: KIND.sink,
          className:
            'oharastream.ohara.it.connector.IncludeAllTypesSinkConnector',
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
  });
});
