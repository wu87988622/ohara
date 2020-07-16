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
import { ElementParameters } from './../../support/customCommands';
import { KIND } from '../../../src/const';
import { NodeRequest } from '../../../src/api/apiInterface/nodeInterface';
import { SOURCE, SINK } from '../../../src/api/apiInterface/connectorInterface';

describe('Navigator', () => {
  const sharedTopicName = generate.serviceName({ prefix: 'topic' });

  before(() => {
    const node: NodeRequest = {
      hostname: generate.serviceName(),
      port: generate.port(),
      user: generate.userName(),
      password: generate.password(),
    };

    cy.deleteAllServices();
    cy.createWorkspace({ node });
    cy.uploadStreamJar();
    cy.createSharedTopic(sharedTopicName);
  });

  context('Outline', () => {
    beforeEach(() => {
      cy.deleteAndStopAllPipelines();
      cy.createPipeline();
    });

    it('should render the UI', () => {
      cy.findByText(/^outline$/i).should('exist');
    });

    it('should render all Paper elements in the list', () => {
      const elements: ElementParameters[] = [
        {
          name: generate.serviceName({ prefix: 'source' }),
          kind: KIND.source,
          className: SOURCE.jdbc,
        },
        {
          name: generate.serviceName({ prefix: 'sink' }),
          kind: KIND.sink,
          className: SINK.hdfs,
        },
        {
          name: sharedTopicName,
          kind: KIND.topic,
        },
        {
          name: 'T1',
          kind: KIND.topic,
        },
        {
          name: generate.serviceName({ prefix: 'stream' }),
          kind: KIND.stream,
        },
      ];

      cy.addElements(elements);

      cy.get('#outline').within(() => {
        cy.get('.list > li').should('have.length', elements.length);
        elements.forEach((element) => {
          cy.findByText(element.name)
            .should('exist')
            .and('have.class', element.kind);
        });
      });
    });

    it('should highlight the selected element', () => {
      const source = generate.serviceName({ prefix: 'source' });
      const sink = generate.serviceName({ prefix: 'sink' });

      cy.addElements([
        { name: source, kind: KIND.source, className: SOURCE.ftp },
        { name: sink, kind: KIND.sink, className: SINK.ftp },
      ]);

      cy.get('#outline').within(() => {
        cy.findByText(source).click().should('have.class', 'is-selected');
      });

      // Element on Paper should also be highlighted
      cy.get('#paper').within(() => {
        cy.findByText(source)
          .parents('.connector')
          .should('have.class', 'is-active');
      });

      // Reset active item
      cy.get('#paper').click();

      // No active item should be found on both Outline and Paper
      cy.get('#outline').within(() => {
        cy.get('.is-selected').should('have.length', 0);
      });

      cy.get('#paper').within(() => {
        cy.get('.is-active').should('have.length', 0);
      });
    });
  });
});
