describe('Home', () => {
  it('loads', () => {
    cy.visit('/');
    cy.get('h1').should('contain', 'Welcome to React');
    cy.get('button').should('contain', 'Hello there!');
  });
});
