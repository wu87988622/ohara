import { KAFKA } from '../../src/constants/urls';

// TODO: skip these tests for now since this will hit the real APIs
// Need to start the Configurator when running on CI
describe('KafkaPage', () => {
  it('creates a new topic', () => {
    cy.visit(KAFKA);

    cy.getByText('New topic').click();

    cy.get('.ReactModal__Content').should('have.length', 1);

    cy.getByLabelText('Topic name').type('test cluster');
    cy.getByLabelText('Partitions').type('1');
    cy.getByLabelText('Replication factor').type('2');
    cy.getByText('Save').click();

    cy.get('.toast-success').should('have.length', 1);
    cy.get('.ReactModal__Content').should('have.length', 0);
    cy.get('td')
      .contains('test cluster')
      .should('have.length', 1);
  });
});
