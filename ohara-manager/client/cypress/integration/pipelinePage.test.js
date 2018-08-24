import * as URLS from '../../src/constants/urls';

describe.skip('PipelinePage', () => {
  beforeEach(() => {
    cy.visit(URLS.PIPELINE);
    cy.get('[data-testid="new-pipeline"]').click();
    cy.get('[data-testid="modal-confirm-btn"]').click();
  });

  it('should be able to navigate to PipelineNewPage', () => {
    cy.location('pathname').should('contain', '/pipeline/new/topic');
    cy.get('[data-testid="graph-list"] li.is-exist').should('have.length', 1);
    cy.get('[data-testid="graph-topic"]')
      .should('be.visible')
      .and('have.class', 'is-active');
  });

  describe('pipeline toolbar', () => {
    it('works correctly', () => {
      cy.get('[data-testid="toolbar-source"]').click();
      cy.get('[data-testid="graph-list"] li.is-exist').should('have.length', 2);
      cy.get('[data-testid="graph-source"]')
        .should('be.visible')
        .and('not.have.class', 'is-active');

      cy.get('[data-testid="toolbar-sink"]').click();
      cy.get('[data-testid="graph-list"] li.is-exist').should('have.length', 3);
      cy.get('[data-testid="graph-source"]')
        .should('be.visible')
        .and('not.have.class', 'is-active');
    });

    it('deletes the pipeline', () => {
      cy.get('[data-testid="delete-pipeline-btn"]').click();
      cy.get('.ReactModal__Content')
        .should('be.visible')
        .find('h3')
        .should('contain', 'Delete pipeline');

      cy.get('[data-testid="confirm-modal-confirm-btn"]').click();

      cy.location('pathname').should('eq', URLS.PIPELINE);
    });
  });

  describe('pipeline graph', () => {
    it('works correctly', () => {
      cy.get('[data-testid="toolbar-source"]').click();
      cy.get('[data-testid="toolbar-sink"]').click();

      cy.get('[data-testid="graph-source"]').click();
      cy.location('pathname').should('contain', '/pipeline/new/source/');

      cy.get('[data-testid="graph-sink"]').click({ force: true });
      cy.location('pathname').should('contain', '/pipeline/new/sink/');

      cy.get('[data-testid="graph-topic"]').click();
      cy.location('pathname').should('contain', '/pipeline/new/topic/');
    });
  });
});
