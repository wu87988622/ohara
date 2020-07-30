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

import { isEmpty, isObject } from 'lodash';
import { CELL_ACTION } from '../../support/customCommands';
import * as generate from '../../../src/utils/generate';
import { NodeRequest } from '../../../src/api/apiInterface/nodeInterface';
import { fetchServices, fetchServiceInfo } from '../../utils';
import { ElementParameters } from './../../support/customCommands';
import {
  SettingDef,
  ClassInfo,
} from '../../../src/api/apiInterface/definitionInterface';
import { KIND } from '../../../src/const';
import { SOURCE, SINK } from '../../../src/api/apiInterface/connectorInterface';

describe('Property view', () => {
  const node: NodeRequest = {
    hostname: generate.serviceName(),
    port: generate.port(),
    user: generate.userName(),
    password: generate.password(),
  };

  before(() => {
    cy.deleteAllServices();
    cy.createWorkspace({ node });

    // A stream is needed for our test to ensure the Toolbox stream list is visible
    cy.uploadStreamJar();
  });

  beforeEach(() => {
    cy.stopAndDeleteAllPipelines();
    cy.createPipeline();
  });

  it('should render Property view UI', () => {
    const sourceName = generate.serviceName({ prefix: 'source' });
    // Create a Perf source
    cy.addElement({
      name: sourceName,
      kind: KIND.source,
      className: SOURCE.perf,
    });

    // Open property view
    cy.getCell(sourceName).click();

    cy.get('#property-view').within(() => {
      // Title
      cy.get('.title-info').within(() => {
        cy.findByText(sourceName).should('exist');

        // Default status since the connector is not started yet
        cy.findByText('Status:').should('exist');
        cy.findByText(/^stopped$/i).should('exist');
      });

      cy.get('.close-button').should('exist');

      // Settings panel is render by default
      cy.findByText('Settings').should('exist');
      cy.findByText('Nodes').should('not.exist');
      cy.findByText('Metrics').should('not.exist');

      // Only settings panel is expanded
      cy.findByTestId('settings-panel')
        .find('.MuiExpansionPanelSummary-root')
        .should('be.visible')
        .and('have.class', 'Mui-expanded');

      cy.get('.MuiExpansionPanel-root.Mui-expanded').should('have.length', 1);
    });
  });

  it('should able to open and close the view', () => {
    const sourceName = generate.serviceName({ prefix: 'source' });
    // Create a Perf source
    cy.addElement({
      name: sourceName,
      kind: KIND.source,
      className: SOURCE.perf,
    });

    // Not visible by default
    cy.get('#property-view').should('not.exist');

    // Open property view and assert
    cy.getCell(sourceName).click();
    cy.get('#property-view').should('exist');

    // Close by clicking on close button and assert
    cy.get('.close-button').click();
    cy.get('#property-view').should('not.exist');

    // Open property view and assert again
    cy.getCell(sourceName).click();
    cy.get('#property-view').should('exist');

    // Close by clicking on paper and assert
    cy.get('#paper').click();
    cy.get('#property-view').should('not.exist');
  });

  it('should reflect the correct status on Property view', () => {
    const { sourceName, topicName } = createSourceAndTopic();

    // Start the connection
    cy.getCell(sourceName).trigger('mouseover');
    cy.cellAction(sourceName, CELL_ACTION.start).click();

    // Open source property view
    cy.getCell(sourceName).click();

    // Assert the status should be running
    cy.get('#property-view .title-info').within(() => {
      cy.findByText(sourceName).should('exist');
      cy.findByText(/^running$/i).should('exist');
    });

    // Close property view
    cy.get('#paper').click();

    // Open topic property view
    cy.getCell(topicName).click();

    // Topic should be running
    cy.get('#property-view .title-info').within(() => {
      cy.findByText(topicName).should('exist');
      cy.findByText(/^running$/i).should('exist');
    });

    // Stop the connection
    cy.getCell(sourceName).trigger('mouseover');
    cy.cellAction(sourceName, CELL_ACTION.stop).click();

    cy.getCell(sourceName).click();

    // Assert the status should be running
    cy.get('#property-view .title-info').within(() => {
      cy.findByText(sourceName).should('exist');
      cy.findByText(/^stopped$/i).should('exist');
    });

    // Open topic property view
    cy.getCell(topicName).click();

    // Topic should still be running
    cy.get('#property-view .title-info').within(() => {
      cy.findByText(topicName).should('exist');
      cy.findByText(/^running$/i).should('exist');
    });
  });

  it('should render topic settings panel', () => {
    // Create a pipeline only topic
    const topicName = 'T1';
    cy.addElement({ name: topicName, kind: KIND.topic });

    // Open property view
    cy.getCell(topicName).click();

    cy.get('#property-view')
      .find('.MuiExpansionPanelDetails-root')
      .then(async ($details) => {
        const objectKey = { group: 'broker', name: 'workspace1' };
        // Need both defs and settings in order to do the assertion
        const topics = await fetchServices(KIND.topic);
        const brokerDefs = await fetchServiceInfo(KIND.topic, objectKey);
        const currentTopic = topics[0];

        const defs = brokerDefs?.classInfos[0]?.settingDefinitions;

        // Some of the defs are hidden from UI
        // 1. internal is not displayed
        // 2. tags is not supported for topic
        // 3. displayName is a custom UI def added by UI. We need to assert it's
        // displayed in the panel as well
        const displayDefs = defs
          .filter((def) => !def.internal)
          .filter((def) => def.key !== 'tags');

        displayDefs.forEach((def: SettingDef) => {
          // Need to handle key conversion here
          const displayName = def.key.indexOf('__')
            ? def.key.replace(/__/g, '.')
            : def.key;

          // Assert all available defs
          cy.wrap($details)
            .findByText(new RegExp(`^${displayName}$`, 'i'))
            .should('exist')
            .next() // display value
            .then(($displayValue) => {
              expect($displayValue.text().replace(/,/g, '')).to.eq(
                String(currentTopic[def.key]),
              );
            });
        });
      });
  });

  // TODO: add more tests for all connectors as well as stream
  it('should render source settings panel', () => {
    // Create a perf source
    const sourceName = generate.serviceName({ prefix: 'source' });
    cy.addElement({
      name: sourceName,
      kind: KIND.source,
      className: SOURCE.perf,
    });

    // Open property view
    cy.getCell(sourceName).click();

    cy.get('#property-view')
      .find('.MuiExpansionPanelDetails-root')
      .then(async ($details) => {
        const objectKey = { group: 'worker', name: 'workspace1' };

        const connectors = await fetchServices(KIND.source);
        const workerDefs = await fetchServiceInfo(KIND.source, objectKey);

        const defs =
          workerDefs.classInfos.find(
            (classInfo: ClassInfo) => classInfo.className === SOURCE.perf,
          )?.settingDefinitions || [];

        const currentConnector = connectors[0]; // we should only have one connector

        // Some of the defs are hidden from UI
        // 1. internal is not displayed
        // 2. tags is not supported for topic
        // 3. displayName is a custom UI def added by UI. We need to assert it's
        // displayed in the panel as well
        const displayDefs = defs
          .filter((def: SettingDef) => !def.internal)
          .filter((def: SettingDef) => def.key !== 'tags');

        displayDefs.forEach((def: SettingDef) => {
          const settingValue = currentConnector[def.key];

          // Empty array is not rendered
          if (isEmpty(settingValue) && isObject(settingValue)) return;

          // Assert all available defs
          cy.wrap($details)
            .findByText(new RegExp(`^${def.displayName}$`, 'i'))
            .should('exist')
            .next() // display value
            .then(($displayValue) => {
              const text = $displayValue.text();

              // UI does conversion from milliseconds -> seconds, we need to
              // convert it back to before making assertion
              if (text.endsWith('seconds')) {
                const calculated = Number(text.split(' ')[0]) * 1000;
                expect(`${calculated} milliseconds`).to.eq(settingValue);
              } else {
                expect(text.replace(/,/g, '')).to.eq(String(settingValue));
              }
            });
        });
      });
  });

  it('should able to render the view with different kind of pipeline components', () => {
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
        name: 'T1',
        kind: KIND.topic,
        className: KIND.topic,
      },
      {
        name: generate.serviceName({ prefix: 'stream' }),
        kind: KIND.stream,
        className: KIND.stream,
      },
    ];

    elements.forEach(({ name, ...rest }) => {
      cy.addElement({ name, ...rest });
      cy.getCell(name).click();

      // The view should be opened
      cy.get('#property-view')
        .find('.title-info')
        .findByText(name)
        .should('exist');

      // Close the view
      cy.get('#paper').click();
    });
  });

  it('should render Nodes panel', () => {
    const { sourceName } = createSourceAndTopic();

    // Start the connection
    cy.getCell(sourceName).trigger('mouseover');
    cy.cellAction(sourceName, CELL_ACTION.start).click();

    // Open property view
    cy.getCell(sourceName).click();

    cy.get('#property-view').within(() => {
      cy.findByText('Nodes').should('exist');

      cy.findByTestId('nodes-panel')
        .find('.MuiExpansionPanelSummary-root')
        .as('summary');

      cy.findByTestId('nodes-panel').within(() => {
        cy.get(' > .MuiExpansionPanelSummary-root').as('summary');

        // Is hidden by default
        cy.get('@summary').should('not.have.class', 'Mui-expanded');

        // Open the content
        cy.get('@summary').click();

        // Nodes panel is now expanded
        cy.get('@summary').should('have.class', 'Mui-expanded');

        // Assert nodes content
        cy.findByText('Coordinators (1)').should('exist');
        cy.findByText('Followers (1)').should('exist');
        cy.findAllByText(/^running$/i).should('have.length', 2);
      });
    });
  });

  it('should not update the topic field of property view before successfully creating topic', () => {
    const sourceName = generate.serviceName({ prefix: 'source' });
    const sinkName = generate.serviceName({ prefix: 'sink' });
    // Create a Shabondi source and sink

    cy.addElements([
      {
        name: sourceName,
        kind: KIND.source,
        className: SOURCE.shabondi,
      },
      {
        name: sinkName,
        kind: KIND.sink,
        className: SINK.shabondi,
      },
    ]);

    // Open source property view
    cy.getCell(sourceName).click();

    // link the source-sink
    cy.cellAction(sourceName, CELL_ACTION.link).click();
    cy.getCell(sinkName).click();

    // target topic should not exist before creating topic
    cy.get('#property-view')
      .find('.MuiExpansionPanelDetails-root')
      .contains('span', 'Target topic')
      .should('not.exist');

    // topic "T1" will be auto created
    cy.findByText('T1')
      .should('exist')
      .parent('.topic')
      .find('.topic-status.running')
      .should('exist'); // Status

    // after topic creation executed successfully
    // we should see the topic field in property view
    cy.get('#property-view')
      .find('.MuiExpansionPanelDetails-root')
      .contains('span', 'Target topic')
      .siblings('span')
      .invoke('html')
      .should('equal', 'T1');
  });
});

function createSourceAndTopic() {
  // Create a Perf source connector and a pipeline only topic
  // then link them together
  const sourceName = generate.serviceName({ prefix: 'source' });
  const topicName = 'T1';
  const elements: ElementParameters[] = [
    {
      name: sourceName,
      kind: KIND.source,
      className: SOURCE.perf,
    },
    {
      name: topicName,
      kind: KIND.topic,
    },
  ];

  cy.addElements(elements);
  cy.createConnections([sourceName, topicName]);

  return { sourceName, topicName };
}
