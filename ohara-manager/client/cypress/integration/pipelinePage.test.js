import * as URLS from '../../src/constants/urls';
import { getTestById } from '../../src/utils/testUtils';

beforeEach(() => {
  cy.visit(URLS.PIPELINE);
  cy.get(getTestById('new-pipeline')).click();
  cy.get(getTestById('modal-confirm-btn')).click();
});

describe.skip('PipelinePage', () => {
  it('should be able to navigate to PipelineNewPage', () => {
    cy.location('pathname').should('contain', '/pipeline/new/topic');
    cy.get('[data-testid="graph-list"] li.is-exist').should('have.length', 1);
    cy.get(getTestById('graph-topic'))
      .should('be.visible')
      .and('have.class', 'is-active');
  });
});

describe.skip('pipeline toolbar', () => {
  it('works correctly', () => {
    cy.get(getTestById('toolbar-source')).click();
    cy.get('[data-testid="graph-list"] li.is-exist').should('have.length', 2);
    cy.get(getTestById('graph-source'))
      .should('be.visible')
      .and('not.have.class', 'is-active');

    cy.get(getTestById('toolbar-sink')).click();
    cy.get('[data-testid="graph-list"] li.is-exist').should('have.length', 3);
    cy.get(getTestById('graph-source'))
      .should('be.visible')
      .and('not.have.class', 'is-active');
  });

  it('deletes the pipeline', () => {
    cy.get(getTestById('delete-pipeline-btn')).click({ force: true });

    cy.get('.ReactModal__Content')
      .should('be.visible')
      .find('h3')
      .should('contain', 'Delete pipeline');

    cy.get(getTestById('confirm-modal-confirm-btn')).click();

    cy.location('pathname').should('eq', URLS.PIPELINE);
  });
});

describe.skip('pipeline graph', () => {
  it('works correctly', () => {
    cy.get(getTestById('toolbar-source')).click();
    cy.get(getTestById('toolbar-sink')).click();

    cy.get(getTestById('graph-source')).click({ force: true });
    cy.location('pathname').should('contain', '/pipeline/new/source/');

    cy.get(getTestById('graph-sink')).click({ force: true });
    cy.location('pathname').should('contain', '/pipeline/new/sink/');

    cy.get(getTestById('graph-topic')).click({ force: true });
    cy.location('pathname').should('contain', '/pipeline/new/topic/');
  });
});
