import { LOGIN, HOME } from '../../src/constants/url';
import { INVALID_USER } from '../../src/constants/cypress';

describe('Login', () => {
  beforeEach(() => {
    cy.visit(LOGIN);
  });

  it('goes to login page', () => {
    cy.location('pathname').should('eq', LOGIN);
  });

  it('shows an error message when logging in with wrong username or password', () => {
    cy.get('[data-testid="username"]').type(INVALID_USER.username);
    cy.get('[data-testid="password"]').type(INVALID_USER.password);
    cy.get('[data-testid="login-form"]').submit();

    cy.get('.toast-error').should('have.length', 1);
  });

  it('redirects to home and displays a success message', () => {
    cy.loginWithUi();

    cy.get('.toast-success').should('contain', '登入成功');
    cy.location('pathname').should('eq', HOME);
  });

  it('changes login text based on user login status', () => {
    cy.get('[data-testid="login-state"]').should('contain', 'Log in');
    cy.loginWithUi();

    cy.get('[data-testid="login-state"]').should('contain', 'Log out');
  });

  it('logs out successfully', () => {
    cy.loginWithUi();

    cy.get('.toast-success').should('contain', '登入成功');
    cy.get('[data-testid="login-state"]').click({ force: true });

    // TODO: A better way to assert this
    cy.get('.toast-success').should('contain', '登出成功');
    cy.get('[data-testid="login-state"]').should('contain', 'Log in');
  });
});
