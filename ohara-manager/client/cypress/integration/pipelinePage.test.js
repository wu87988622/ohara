describe.skip('PipelinePage', () => {
  beforeEach(() => {
    cy.visit('/pipeline');
    cy.get('[data-testid="new-pipeline"]').click();
    cy.get('[data-testid="modal-confirm-button"]').click();
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
  });

  describe('pipeline graph', () => {
    it('works correctly', () => {});
  });
});
