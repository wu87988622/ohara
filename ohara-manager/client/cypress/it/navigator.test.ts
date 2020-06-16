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
import { deleteAllServices } from '../utils';
import { SOURCES, SINKS } from '../../src/api/apiInterface/connectorInterface';
import { KIND } from '../../src/const';
import { NodeRequest } from '../../src/api/apiInterface/nodeInterface';

describe('Navigator', () => {
  before(() => {
    const node: NodeRequest = {
      hostname: generate.serviceName(),
      port: generate.port(),
      user: generate.userName(),
      password: generate.password(),
    };

    deleteAllServices();
    cy.createWorkspace({ node });
  });

  describe('Pipeline list', () => {
    beforeEach(() => {
      cy.deleteAllPipelines();
    });

    it('should render the UI', () => {
      cy.get('#navigator').within(() => {
        cy.findByText('Pipelines').should('exist');
        cy.findByTestId('new-pipeline-button').should('exist');
        cy.findByTestId('pipeline-list-expand-button').should('exist');
      });
    });

    it('should able to expand and collapse the list', () => {
      const pipeline1 = generate.serviceName({ prefix: 'pi' });

      cy.createPipeline(pipeline1);
      cy.findByText(pipeline1).should('exist');
      cy.findByTestId('pipeline-list-expand-button').click();
      cy.findByText(pipeline1).should('not.be.visible');

      // Set it back to expanded
      cy.findByTestId('pipeline-list-expand-button').click();
    });

    it('should list all pipelines under the current workspace', () => {
      const pipeline1 = generate.serviceName({ prefix: 'pi' });
      const pipeline2 = generate.serviceName({ prefix: 'pi' });

      cy.createPipeline(pipeline1);
      cy.createPipeline(pipeline2);

      cy.get('#pipeline-list > li').should('have.length', 2);

      // The last created item should be the active one
      cy.findByText(pipeline2).should('have.class', 'active-link');
    });

    it('should able to switch between pipelines', () => {
      const pipeline1 = generate.serviceName({ prefix: 'pi' });
      const pipeline2 = generate.serviceName({ prefix: 'pi' });
      const pipeline3 = generate.serviceName({ prefix: 'pi' });

      cy.createPipeline(pipeline1);
      cy.createPipeline(pipeline2);
      cy.createPipeline(pipeline3);

      cy.get('#pipeline-list > li').should('have.length', 3);

      // The last item should be the active item
      cy.findByText(pipeline3).should('have.class', 'active-link');

      cy.location('pathname').should('include', pipeline3);

      // Switching to first one
      cy.findByText(pipeline1).click();
      cy.location('pathname').should('include', pipeline1);

      // Switching to second one
      cy.findByText(pipeline2).click();
      cy.location('pathname').should('include', pipeline2);

      // Reloading page should maintain the same URL
      cy.reload();
      cy.location('pathname').should('include', pipeline2);
      cy.findByText(pipeline2).should('have.class', 'active-link');
    });

    it('should sort the pipeline by alphabet', () => {
      const pipeline1 = 'pipeline1';
      const pipeline2 = 'pipeline2';
      const pipeline3 = 'pipeline3';

      cy.createPipeline(pipeline1);
      cy.createPipeline(pipeline2);
      cy.createPipeline(pipeline3);

      cy.get('#pipeline-list > li').should('have.length', 3);

      cy.get('#pipeline-list > li').eq(0).should('have.text', pipeline1);
      cy.get('#pipeline-list > li').eq(1).should('have.text', pipeline2);
      cy.get('#pipeline-list > li').eq(2).should('have.text', pipeline3);
    });

    it('should set a default pipeline when the pipeline name from URL could not be met', () => {
      const pipeline1 = 'pipeline1';
      const pipeline2 = 'pipeline2';
      cy.createPipeline(pipeline1);
      cy.createPipeline(pipeline2);

      cy.get('#pipeline-list > li').should('have.length', 2);

      // Currently, at pipeline2
      cy.location('pathname').should('include', pipeline2);
      cy.findByText(pipeline2).should('have.class', 'active-link');

      // Visit a URL with none-existent pipeline name
      cy.visit('workspace1/abcdefg');

      // Should redirect back to the default pipeline
      cy.location('pathname').should('include', pipeline1);
      cy.findByText(pipeline1).should('have.class', 'active-link');
    });

    it('should reset active pipeline when current one is deleted', () => {
      const pipeline1 = 'pipeline1';
      const pipeline2 = 'pipeline2';

      cy.createPipeline(pipeline1);
      cy.createPipeline(pipeline2);

      cy.get('#pipeline-list > li').should('have.length', 2);

      // Default
      cy.location('pathname').should('include', pipeline2);
      cy.findByText(pipeline2).should('have.class', 'active-link');

      cy.deletePipeline(pipeline2);

      // After `pipeline1` was deleted, the pipeline should be reset
      cy.location('pathname').should('include', pipeline1);
      cy.findByText(pipeline1).should('have.class', 'active-link');
    });
  });

  describe('Create pipeline dialog', () => {
    it('should render the UI', () => {
      cy.findByTestId('new-pipeline-button').click();

      cy.findByTestId('new-pipeline-dialog').within(() => {
        // Heading and close button
        cy.findByText(/^add a new pipeline$/i).should('exist');
        cy.findByTestId('close-button').should('exist');

        // Actions
        cy.findByText(/^add$/i).parent('button').should('be.disabled');
        cy.findByText(/^cancel$/i).should('exist');

        // Form
        cy.findByText(/^pipeline name$/i).should('exist');
        cy.findByPlaceholderText(/^pipeline1$/i).should('exist');

        // Close the dialog so it won't interfere the following tests
        cy.findByTestId('close-button').click();
      });
    });

    it('should able to close dialog with close button and cancel button', () => {
      const newBtn = 'new-pipeline-button';
      const closeBtn = 'close-button';
      const dialog = 'new-pipeline-dialog';

      cy.findByTestId(newBtn).click();

      cy.findByTestId(dialog).should('be.visible');

      cy.findByTestId(closeBtn).click();
      cy.findByTestId(dialog).should('not.be.visible');

      cy.findByTestId(newBtn).click();
      cy.findByTestId(dialog).should('be.visible');

      cy.findByText(/^cancel$/i).click();
      cy.findByTestId(dialog).should('not.be.visible');
    });

    it('should validate the input pipeline name', () => {
      const invalidLongName = generate.serviceName({
        prefix: 'pi',
        length: 40,
      });
      const invalidShortName = 'x';
      const invalidName = '⛔️';
      const validName = generate.serviceName({ prefix: 'pi', length: 10 });
      const TooLongError =
        'The value must be less than or equal to 20 characters long';
      const TooShortError =
        'The value must be greater than or equal to 2 characters long';
      const invalidCharError =
        'You only can use lower case letters and numbers';
      const requireFieldError = 'This is a required field';

      cy.findByTestId('new-pipeline-button').click();
      cy.findByTestId('new-pipeline-dialog').find('input').as('input');

      // Too long
      cy.get('@input').type(invalidLongName).blur();
      cy.findByText(TooLongError).should('exist');
      cy.findByText(/^add$/i).parent('button').should('be.disabled');

      // Invalid character
      cy.get('@input').clear().type(invalidName).blur();
      cy.findByText(invalidCharError).should('exist');
      cy.findByText(/^add$/i).parent('button').should('be.disabled');

      // Too short
      cy.get('@input').clear().type(invalidShortName).blur();
      cy.findByText(TooShortError).should('exist');
      cy.findByText(/^add$/i).parent('button').should('be.disabled');

      cy.get('@input').clear().blur();
      cy.findByText(requireFieldError).should('exist');
      cy.findByText(/^add$/i).parent('button').should('be.disabled');

      // valid name, we should able to create a new pipeline with it
      cy.get('@input').clear().type(validName);
      cy.findByText(/^add$/i)
        .parent('button')
        .should('not.be.disabled')
        .click();

      cy.findByText(validName).should('exist');
    });
  });

  describe('Outline', () => {
    beforeEach(() => {
      cy.deleteAllPipelines();
      cy.createPipeline();
    });

    it('should render the UI', () => {
      cy.findByText(/^outline$/i).should('exist');
    });

    it('should render all Paper elements in the list', () => {
      const sources = Object.values(SOURCES).map((type) => ({
        name: generate.serviceName({ prefix: 'source' }),
        kind: KIND.source,
        type,
      }));

      const sinks = Object.values(SINKS).map((type) => ({
        name: generate.serviceName({ prefix: 'sink' }),
        kind: KIND.sink,
        type,
      }));

      const elements = [...sources, ...sinks];

      // Scroll bar is not visible when Outline list is empty or not long enough
      // to display it
      cy.get('.ScrollbarsCustom-TrackY').should('not.be.visible');

      elements.forEach(({ name, kind, type }) => {
        cy.addElement(name, kind, type);
      });

      // While we're here, let's assert the scroll bar is properly displayed when
      // there are too many items
      cy.get('.ScrollbarsCustom-TrackY').should('be.visible');

      cy.get('#outline').within(() => {
        elements.forEach((element) => {
          cy.findByText(element.name).should('exist');
        });
      });
    });

    it('should highlight the selected element', () => {
      const source = generate.serviceName({ prefix: 'source' });
      const sink = generate.serviceName({ prefix: 'sink' });

      cy.addElement(source, KIND.source, SOURCES.ftp);
      cy.addElement(sink, KIND.sink, SINKS.ftp);

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
