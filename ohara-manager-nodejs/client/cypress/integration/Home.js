describe('Home', () => {
  it('loads', () => {
    cy.visit('/');
    cy.location('pathname').should('eq', '/');
  });
});
