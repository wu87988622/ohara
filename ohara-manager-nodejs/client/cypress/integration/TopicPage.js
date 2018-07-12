describe('TopicPage', () => {
  it('loads', () => {
    cy.visit('/topics');
    cy.get('[data-test]').should('contain', 'See the topics here!');
  });
});
