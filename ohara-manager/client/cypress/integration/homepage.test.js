import { HOME } from '../../src/constants/url';

describe('HomePage', () => {
  it('loads with the correct URL', () => {
    cy.visit(HOME);
    cy.location('pathname').should('eq', HOME);
  });
});
