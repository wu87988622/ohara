import * as URLS from '../../src/constants/urls';

describe('configuration page', () => {
  beforeEach(() => {
    cy.visit(URLS.CONFIGURATION);
  });

  it('should go to configuration page', () => {
    cy.location('pathname').should('eq', URLS.CONFIGURATION);
  });

  it('should pass the connection test with correct HDFS info', () => {
    cy.getByLabelText('Name')
      .clear()
      .type('test connection');
    cy.getByLabelText('HDFS connection URL')
      .clear()
      .type('file://test/connection');

    cy.getByText('Test connection').click();

    cy.wait(3000);
    cy.get('.toast-success').should('have.length.above', 1);
  });

  it('should not pass the connection with wrong HDFS info', () => {
    cy.getByLabelText('Name')
      .clear()
      .type('test connection');
    cy.getByLabelText('HDFS connection URL')
      .clear()
      .type('somethingwrongaboutthisurl');

    cy.getByText('Test connection').click();
    cy.wait(3000);
    cy.get('.toast-error').should('have.length', 1);
  });
});
