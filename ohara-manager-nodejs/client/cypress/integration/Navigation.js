describe('Navigation', () => {
  it('works with address bar', () => {
    cy.visit('/')
      .location('pathname')
      .should('eq', '/');

    cy.visit('/schema')
      .location('pathname')
      .should('eq', '/schema');

    cy.visit('/topics')
      .location('pathname')
      .should('eq', '/topics');
  });

  it('works with mouse click', () => {
    cy.visit('/')
      .location('pathname')
      .should('eq', '/');

    cy.get('[data-test="nav-schema"]')
      .click()
      .location('pathname')
      .should('eq', '/schema');

    cy.get('[data-test="nav-topics"]')
      .click()
      .location('pathname')
      .should('eq', '/topics');

    cy.get('[data-test="nav-home"]')
      .click()
      .location('pathname')
      .should('eq', '/');
  });
});
