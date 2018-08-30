import * as URLS from '../../src/constants/urls';
import { getTestById } from '../../src/utils/testHelpers';

describe.skip('configuration page', () => {
  beforeEach(() => {
    cy.visit(URLS.CONFIGURATION);
  });

  it('should go to configuration page', () => {
    cy.location('pathname').should('eq', URLS.CONFIGURATION);
  });

  it('should pass the test with correct HDFS info', () => {
    cy.get(getTestById('connection-name-input'))
      .clear()
      .type('test connection');
    cy.get(getTestById('connection-url-input'))
      .clear()
      .type('file://test/connection');

    cy.get(getTestById('test-connection-btn')).click();

    cy.wait(3000);
    cy.get('.toast-success').should('have.length.above', 1);
  });
});
