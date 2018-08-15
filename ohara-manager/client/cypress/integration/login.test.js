import { LOGIN, HOME } from '../../src/constants/url';

const invalidUser = {
  username: 'invalid@gmail.com',
  password: '123456',
};

const validUser = {
  username: 'joshua',
  password: '111111',
};

describe('Login', () => {
  it('goes to login page', () => {
    cy.visit(LOGIN);
    cy.location('pathname').should('eq', LOGIN);
  });

  // TODO: Refactor login command, make it DRI
  it('shows an error message when logging in with wrong username or password', () => {
    cy.visit(LOGIN);

    cy.get('[data-testid="username"]').type(invalidUser.username);
    cy.get('[data-testid="password"]').type(invalidUser.password);
    cy.get('[data-testid="login-form"]').submit();

    cy.get('.toast-error').should('have.length', 1);
  });

  it('redirects to home and displays a success message', () => {
    cy.visit(LOGIN);

    cy.get('[data-testid="username"]').type(validUser.username);
    cy.get('[data-testid="password"]').type(validUser.password);
    cy.get('[data-testid="login-form"]').submit();

    cy.get('.toast-success').should('have.length', 1);
    cy.location('pathname').should('eq', HOME);
  });

  it('changes login text based on user login status', () => {
    cy.visit(LOGIN);

    cy.get('[data-testid="login-state"]').should('contain', 'Log in');

    cy.get('[data-testid="username"]').type(validUser.username);
    cy.get('[data-testid="password"]').type(validUser.password);
    cy.get('[data-testid="login-form"]').submit();

    cy.get('[data-testid="login-state"]').should('contain', 'Log out');
  });

  it('logs out successfully', () => {
    cy.visit(LOGIN);

    cy.get('[data-testid="username"]').type(validUser.username);
    cy.get('[data-testid="password"]').type(validUser.password);
    cy.get('[data-testid="login-form"]').submit();

    cy.get('.toast-success').should('have.length', 1);

    cy.get('[data-testid="login-state"]').click({ force: true });

    // TODO: A better way to assert this
    cy.get('.toast-success').should('have.length.above', 1);
  });
});
