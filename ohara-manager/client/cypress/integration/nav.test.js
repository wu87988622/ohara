import { HOME, PIPELINE, KAFKA, CONFIGURATION } from '../../src/constants/url';

describe('Nav', () => {
  it('matches the correct routes', () => {
    cy.visit('/');
    cy.location('pathname').should('eq', HOME);

    cy.get('[data-testid=nav-pipeline]').click();
    cy.location('pathname').should('eq', PIPELINE);

    cy.get('[data-testid=nav-kafka]').click();
    cy.location('pathname').should('eq', KAFKA);

    cy.get('[data-testid=nav-configuration]').click();
    cy.location('pathname').should('eq', CONFIGURATION);

    cy.get('[data-testid=brand]').click();
    cy.location('pathname').should('eq', HOME);
  });
});
