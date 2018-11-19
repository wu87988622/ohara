import * as URLS from '../../src/constants/urls';
import { getTestById } from '../../src/utils/testUtils';

describe('Nav', () => {
  it('matches the correct routes', () => {
    cy.visit(URLS.HOME);

    cy.get(getTestById('pipelines-link')).click();
    cy.location('pathname').should('eq', URLS.PIPELINE);

    cy.get(getTestById('configuration-link')).click();
    cy.location('pathname').should('eq', URLS.CONFIGURATION);

    cy.get(getTestById('deployment-link')).click();
    cy.location('pathname').should('eq', URLS.DEPLOYMENT);

    cy.get(getTestById('monitoring-link')).click();
    cy.location('pathname').should('eq', URLS.MONITORING);
  });
});
