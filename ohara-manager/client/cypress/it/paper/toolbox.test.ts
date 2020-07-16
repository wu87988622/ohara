import { ElementParameters } from './../../support/customCommands';
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
import { NodeRequest } from '../../../src/api/apiInterface/nodeInterface';
import { KIND, CELL_TYPE } from '../../../src/const';
import { SOURCE, SINK } from '../../../src/api/apiInterface/connectorInterface';

const sources = Object.values(SOURCE).sort((a, b) => a.localeCompare(b));
const sinks = Object.values(SINK).sort((a, b) => a.localeCompare(b));

describe('ToolBox', () => {
  const node: NodeRequest = {
    hostname: generate.serviceName(),
    port: generate.port(),
    user: generate.userName(),
    password: generate.password(),
  };

  const sharedTopicName = generate.serviceName({ prefix: 'topic' });
  before(() => {
    cy.deleteAllServices();
    cy.createWorkspace({ node });
    cy.createSharedTopic(sharedTopicName);
    cy.uploadStreamJar();
  });

  beforeEach(() => {
    cy.deleteAndStopAllPipelines();
    cy.createPipeline();
  });

  context('Header', () => {
    // TODO: add a drag and drop header test

    it('should render the title and related UI', () => {
      cy.get('#toolbox').within(() => {
        cy.findByText(/^toolbox$/i).should('exist');
        cy.findByTestId('search-icon').should('exist');
        cy.findByPlaceholderText('Search topic & connector...').should('exist');

        cy.findByTestId('close-button').should('exist');
      });
    });

    it('should able to hide Toolbox', () => {
      cy.get('#toolbox')
        .should('be.visible')
        .within(() => {
          cy.findByTestId('close-button').click();
        });

      cy.get('#toolbox').should('not.be.visible');
    });
  });

  context('Search', () => {
    it('should able to search components', () => {
      cy.get('#toolbox').within(() => {
        cy.findByPlaceholderText('Search topic & connector...').as('input');

        cy.get('@input').type('ftp');
        cy.findByText('FtpSource').should('exist');
        cy.findByText('FtpSink').should('exist');
        cy.get('#source-list').should('be.visible');
        cy.get('#sink-list').should('be.visible');
        cy.get('.toolbox-list:visible').should('have.length', 2);

        cy.get('@input').clear().type('console');
        cy.findByText('ConsoleSink').should('exist');
        cy.get('#sink-list').should('be.visible');
        cy.get('.toolbox-list:visible').should('have.length', 1);

        cy.get('@input').clear().type('only');
        cy.findByText(/^pipeline only$/i).should('exist');
        cy.get('#topic-list').should('be.visible');
        cy.get('.toolbox-list:visible').should('have.length', 1);

        cy.get('@input').clear().type('thisisnottherealname');
        cy.get('.toolbox-list:visible').should('have.length', 0);
      });
    });

    it('should able to clear search term with escape key', () => {
      cy.get('#toolbox').within(() => {
        cy.findByPlaceholderText('Search topic & connector...').as('input');
        cy.get('@input').type('abcdefg');
        cy.get('@input').type('{esc}');
        cy.get('@input').should('be.empty');
      });
    });
  });

  context('Toolbox panels', () => {
    it('should render all collapsed panels', () => {
      cy.get('#toolbox').within(() => {
        // All panels are collapsed by default
        cy.get('.toolbox-list:visible').should('have.length', 0);

        cy.get('.toolbox-body').children().should('have.length', 4);
        cy.findByText(/^source$/i).should('exist');
        cy.findByText(/^topic$/i).should('exist');
        cy.findByText(/^stream$/i).should('exist');
        cy.findByText(/^sink$/i).should('exist');
      });
    });

    it('should render source panel', () => {
      cy.get('#source-panel').within(() => {
        cy.findByText(/^source$/i).click();

        cy.findByText(/^Add source connectors$/i).should('exist');
        cy.get('.add-button').should('exist');

        // Ensure all official sources connectors are rendered
        cy.get('#source-list .item').should('have.length', sources.length);
        Object.values(sources).forEach((className) => {
          const name = className.slice(className.lastIndexOf('.') + 1);
          cy.findByText(name).should('exist');
        });
      });
    });

    it('should render topic panel', () => {
      cy.get('#topic-panel').within(() => {
        cy.findByText(/^topic$/i).click();

        cy.get('.add-button').should('exist');
        cy.findByText(/^Add topics$/i).should('exist');

        // Should only have a pipeline only topic item
        cy.get('#topic-list > .item').should('have.length', 2);
        cy.findByText(/^Pipeline only$/i).should('exist');
      });
    });

    it('should render stream panel', () => {
      cy.get('#stream-panel').within(() => {
        cy.findByText(/^stream$/i).click();

        cy.get('.add-button').should('exist');
        cy.findByText(/^DumbStream$/i).should('exist');

        // Defaults to empty
        cy.get('#stream-list > .item').should('have.length', 1);
        cy.findByText(/^Add streams$/i).should('exist');
      });
    });

    it('should render sink panel', () => {
      cy.get('#sink-panel').within(() => {
        cy.findByText(/^sink$/i).click();

        cy.findByText('Add sink connectors').should('exist');
        cy.get('.add-button').should('exist');

        // Ensure all official sinks connectors are rendered
        cy.get('#sink-list .item').should('have.length', sinks.length);
        Object.values(sinks).forEach((className) => {
          const name = className.slice(className.lastIndexOf('.') + 1);
          cy.findByText(name).should('exist');
        });
      });
    });

    it('should redirect to settings page by clicking on the add button', () => {
      // TODO: remove the repetitive part of the test
      cy.get('#source-panel').within(() => {
        cy.findByText(/^source$/i).click();
        cy.get('.add-button > button').click();
      });

      assertPageTitle('Worker plugins and shared jars');

      cy.get('#topic-panel').within(() => {
        cy.findByText(/^topic$/i).click();
        cy.get('.add-button > button').click();
      });

      assertPageTitle('Topics in this workspace');

      cy.get('#stream-panel').within(() => {
        cy.findByText(/^stream$/i).click();
        cy.get('.add-button > button').click();
      });

      assertPageTitle('Stream jars');

      cy.get('#sink-panel').within(() => {
        cy.findByText(/^sink$/i).click();
        cy.get('.add-button > button').click();
      });

      assertPageTitle('Worker plugins and shared jars');
    });
  });

  context('Toolbox interaction with Paper', () => {
    it('should able to create Paper elements with Toolbox items', () => {
      const sources = Object.values(SOURCE).map((className) => ({
        name: generate.serviceName({ prefix: 'source' }),
        kind: KIND.source,
        className,
      }));

      const sinks = Object.values(SINK).map((className) => ({
        name: generate.serviceName({ prefix: 'sink' }),
        kind: KIND.sink,
        className,
      }));

      const streams = [
        {
          name: 'stream',
          kind: KIND.stream,
          className: KIND.stream,
        },
      ];

      const topics = [
        {
          name: sharedTopicName,
          kind: KIND.topic,
          className: KIND.topic,
        },
        { name: 'T1', kind: KIND.topic },
        { name: 'T2', kind: KIND.topic },
      ];

      cy.addElements([
        ...sources,
        ...sinks,
        ...streams,
        ...topics,
      ] as ElementParameters[]);

      cy.get('#paper').within(() => {
        cy.get('.stream').should('have.length', streams.length);
        cy.get('.topic').should('have.length', topics.length);
        cy.get('.connector').should(
          'have.length',
          sources.length + sinks.length,
        );
      });
    });

    it('should disable the shared topic in Toolbox if the topic is used in the pipeline', () => {
      cy.addElement({ name: sharedTopicName, kind: KIND.topic });

      // Disabled when it's used
      cy.get('#toolbox').within(() => {
        cy.findByText(/^topic$/i).click();
        cy.findByText(sharedTopicName)
          .parents('.item')
          .should('have.class', 'is-disabled');
      });

      // Remove it
      cy.removeElement(sharedTopicName);

      // Available again since it's removed from Paper
      cy.get('#toolbox').within(() => {
        cy.findByText(/^topic$/i).click();
        cy.findByText(sharedTopicName)
          .parents('.item')
          .should('not.have.class', 'is-disabled');
      });
    });

    it('should not add an element if the element is over Toolbox', () => {
      // Open topic panel
      cy.findByText(Cypress._.capitalize(KIND.topic)).should('exist').click();

      // Add a pipeline-only topic
      cy.findByTestId('toolbox-draggable')
        .find(`g[data-type="${CELL_TYPE.ELEMENT}"]:visible`)
        // the only "draggable" cell is pipeline-only topic
        .first()
        .dragAndDrop(30, 450);

      // No topic is added
      cy.get('#paper .paper-element').should('have.length', 0);
    });

    it('should fail to add a shared topic into Paper if the name is already taken', () => {
      const name = 'taken';

      // Create a topic with the name
      cy.createSharedTopic(name);

      // Add a sink into Paper with the same name
      cy.addElement({ name, kind: KIND.sink, className: SINK.shabondi });

      // Then, add the topic
      cy.findByText(Cypress._.capitalize(KIND.topic)).should('exist').click();

      // Add topic
      cy.findByTestId('toolbox-draggable')
        .within(() => {
          cy.findByText(name)
            .should('exist')
            .and('have.class', 'display-name')
            .parent('.item')
            .should('have.attr', 'data-testid')
            .then((testId) => cy.get(`g[model-id="${testId}"]`));
        })
        .dragAndDrop(800, 500); // just a position to place the topic

      // A warning should display
      cy.findByText(
        `The name "${name}" is already taken, use another name for your shared topic instead!`,
      ).should('exist');

      // Should only have an element in the Paper
      cy.get('#paper .paper-element').should('have.length', 1);
    });
  });
});

function assertPageTitle(pageTitle: string) {
  // We're now in the settings page
  cy.findByTestId('workspace-settings-dialog').within(() => {
    cy.findByText(/^settings$/i).should('exist');
    cy.get('.section-page-header').should('have.text', pageTitle);

    cy.findByTestId('workspace-settings-dialog-close-button').click({
      force: true,
    });
  });
}
