import { KAFKA } from '../../src/constants/urls';

// TODO: skip these tests for now since this will hit the real APIs
// Need to start the Configurator when running on CI
describe.skip('KafkaPage', () => {
  it('creates a new topic', () => {
    cy.visit(KAFKA);

    cy.get('[data-testid="new-topic"]').click();

    cy.get('.ReactModal__Content').should('have.length', 1);

    cy.get('[data-testid="modal-cluster-name"]').type('test cluster');
    cy.get('[data-testid="modal-partitions"]').type('1');
    cy.get('[data-testid="modal-replication-factor"]').type('2');
    cy.get('[data-testid="modal-submit-btn"]').click();

    cy.get('.toast-success').should('have.length', 1);
    cy.get('.ReactModal__Content').should('have.length', 0);
    cy.get('td')
      .contains('test cluster')
      .should('have.length', 1);
  });
});
