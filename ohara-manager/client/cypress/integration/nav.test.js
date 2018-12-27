import * as URLS from '../../src/constants/urls';

describe('Header', () => {
  beforeEach(() => {
    cy.visit(URLS.HOME);
  });

  it('visits all pages from main navigation', () => {
    cy.get('nav').within(() => {
      cy.getByText('Pipelines').click();
      cy.location('pathname').should('eq', URLS.PIPELINE);

      cy.getByText('Nodes').click();
      cy.location('pathname').should('eq', URLS.NODES);

      cy.getByText('Services').click();
      cy.location('pathname').should('eq', URLS.SERVICES);

      cy.getByText('Monitoring').click();
      cy.location('pathname').should('eq', URLS.MONITORING);
    });
  });

  it('visits Log in page', () => {
    cy.getByText('Log in').click();
    cy.location('pathname').should('eq', URLS.LOGIN);
  });

  it('toggles configuration modal', () => {
    cy.getByTestId('config-btn').click();
    cy.get('.ReactModal__Content')
      .contains('Configuration')
      .should('be.visible');

    cy.getByTestId('close-btn').click();

    cy.get('.ReactModal__Content').should('not.be.visible');
  });
});
