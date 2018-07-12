describe('SchemaPage', () => {
  it('loads', () => {
    cy.visit('/schema');
    cy.location('pathname').should('eq', '/schema');
  });

  it('toggles SchemaPage class isActive correctly', () => {
    const activeCls = 'isActive';
    const activeText = `I'm Schema page, App is ready ? Yes`;
    const disableText = `I'm Schema page, App is ready ? Nope`;
    cy.get('.SchemaPage').as('page');

    cy.get('@page')
      .should('have.class', activeCls)
      .should('contain', activeText)
      .click()
      .should('not.have.class', activeCls)
      .should('contain', disableText)
      .click()
      .should('have.class', activeCls)
      .should('contain', activeText);
  });
});
