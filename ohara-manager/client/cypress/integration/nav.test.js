import * as URLS from '../../src/constants/urls';
import { getTestById } from '../../src/utils/testHelpers';

describe('Nav', () => {
  it('matches the correct routes', () => {
    cy.visit('/');
    cy.location('pathname').should('eq', URLS.HOME);

    cy.get(getTestById('nav-pipeline')).click();
    cy.location('pathname').should('eq', URLS.PIPELINE);

    cy.get(getTestById('nav-kafka')).click();
    cy.location('pathname').should('eq', URLS.KAFKA);

    cy.get(getTestById('nav-configuration')).click();
    cy.location('pathname').should('eq', URLS.CONFIGURATION);

    cy.get(getTestById('brand')).click();
    cy.location('pathname').should('eq', URLS.HOME);
  });
});
