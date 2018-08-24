import * as URLS from '../../src/constants/urls';

describe.skip('configuration page', () => {
  beforeEach(() => {
    cy.visit(URLS.CONFIGURATION);
  });

  it('should go to configuration page', () => {
    cy.location('pathname').should('eq', URLS.CONFIGURATION);
  });

  it('should pass the test with correct HDFS info', () => {
    cy.get('[data-testid="connection-name-input"]')
      .clear()
      .type('test connection');
    cy.get('[data-testid="connection-url-input"]')
      .clear()
      .type('file://test/connection');

    cy.get('[data-testid="test-connection-btn"]').click();

    cy.wait(3000);
    cy.get('.toast-success').should('have.length.above', 1);
  });
});
