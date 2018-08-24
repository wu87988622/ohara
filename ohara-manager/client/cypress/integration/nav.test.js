import * as URLS from '../../src/constants/urls';

describe('Nav', () => {
  it('matches the correct routes', () => {
    cy.visit('/');
    cy.location('pathname').should('eq', URLS.HOME);

    cy.get('[data-testid=nav-pipeline]').click();
    cy.location('pathname').should('eq', URLS.PIPELINE);

    cy.get('[data-testid=nav-kafka]').click();
    cy.location('pathname').should('eq', URLS.KAFKA);

    cy.get('[data-testid=nav-configuration]').click();
    cy.location('pathname').should('eq', URLS.CONFIGURATION);

    cy.get('[data-testid=brand]').click();
    cy.location('pathname').should('eq', URLS.HOME);
  });
});
