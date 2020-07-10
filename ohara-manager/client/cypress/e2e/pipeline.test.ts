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
import { ElementParameters, CELL_ACTION } from '../support/customCommands';
import { SOURCE, SINK } from '../../src/api/apiInterface/connectorInterface';

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
            .find('.metrics-value')
            .should(($value) => {
              expect(Number($value.text())).to.be.greaterThan(0);
            });
        });
    });
  });
});
