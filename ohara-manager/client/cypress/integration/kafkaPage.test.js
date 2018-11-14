import { KAFKA } from '../../src/constants/urls';
import { getTestById } from '../../src/utils/testUtils';

// TODO: skip these tests for now since this will hit the real APIs
// Need to start the Configurator when running on CI
describe.skip('KafkaPage', () => {
  it('creates a new topic', () => {
    cy.visit(KAFKA);

    cy.get(getTestById('new-topic')).click();

    cy.get('.ReactModal__Content').should('have.length', 1);

    cy.get(getTestById('modal-cluster-name')).type('test cluster');
    cy.get(getTestById('modal-partitions')).type('1');
    cy.get(getTestById('modal-replication-factor')).type('2');
    cy.get(getTestById('modal-submit-btn')).click();

    cy.get('.toast-success').should('have.length', 1);
    cy.get('.ReactModal__Content').should('have.length', 0);
    cy.get('td')
      .contains('test cluster')
      .should('have.length', 1);
  });
});
