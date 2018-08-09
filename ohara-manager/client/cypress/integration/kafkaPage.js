import { HOME, KAFKA } from '../../src/constants/url';

// TODO: skip these tests for now since this will hit the real APIs
// Need to start the Configurator when running on CI
describe.skip('KafkaPage', () => {
  it('tests connection should be passed', () => {
    cy.visit(KAFKA);

    cy.get('[data-testid="clusterName"]').type('test cluster');
    cy.get('[data-testid="brokerList"]').type('http://localhost:4040');

    cy.get('[data-testid="testConnection"]').click();
    cy.get('.toast-success').should('have.length', 1);
  });

  it('gets back to the previous page in the history when clicking on cancel', () => {
    cy.visit(HOME);
    cy.visit(KAFKA);
    cy.get('[data-testid="cancelButton"]').click();
    cy.location('pathname').should('eq', HOME);
  });

  it('creates a new topic', () => {
    cy.visit(KAFKA);

    cy.get('[data-testid="createTopic"]').click();

    cy.get('.ReactModal__Content').should('have.length', 1);

    cy.get('[data-testid="modalClusterName"]').type('test cluster');
    cy.get('[data-testid="modalPartitions"]').type('1');
    cy.get('[data-testid="modalReplicationFactor"]').type('2');
    cy.get('[data-testid="modalSubmitButton"]').click();

    cy.get('.toast-success').should('have.length', 1);
    cy.get('.ReactModal__Content').should('have.length', 0);
    cy.get('td')
      .contains('test cluster')
      .should('have.length', 1);
  });
});
