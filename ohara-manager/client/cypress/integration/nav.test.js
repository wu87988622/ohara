import * as URLS from '../../src/constants/urls';
import { getTestById } from '../../src/utils/testUtils';

describe('Header', () => {
  beforeEach(() => {
    cy.visit(URLS.HOME);
  });

  it('matches the correct routes', () => {
    cy.get('nav')
      .contains('Pipelines')
      .click();
    cy.location('pathname').should('eq', URLS.PIPELINE);

    cy.get('nav')
      .contains('Deployment')
      .click();
    cy.location('pathname').should('eq', URLS.DEPLOYMENT);

    cy.get('nav')
      .contains('Monitoring')
      .click();
    cy.location('pathname').should('eq', URLS.MONITORING);

    cy.get('header')
      .contains('Log in')
      .click();
    cy.location('pathname').should('eq', URLS.LOGIN);
  });

  it('toggles configuration modal', () => {
    cy.get(getTestById('config-btn')).click();
    cy.get('.ReactModal__Content')
      .contains('Configuration')
      .should('be.visible');

    cy.get('.ReactModal__Content')
      .find(getTestById('close-btn'))
      .click();

    cy.get('.ReactModal__Content').should('not.be.visible');
  });
});
