import { setUserKey } from '../../src/utils/authUtils';
import { VALID_USER } from '../../src/constants/cypress';
import * as _ from '../../src/utils/commonUtils';

Cypress.Commands.add('loginWithUi', () => {
  cy.get('[data-testid="username"]').type(VALID_USER.username);
  cy.get('[data-testid="password"]').type(VALID_USER.password);
  cy.get('[data-testid="login-form"]').submit();
});

Cypress.Commands.add('login', () => {
  cy.request({
    method: 'POST',
    url: 'http://localhost:5050/api/login',
    body: {
      username: VALID_USER.username,
      password: VALID_USER.password,
    },
  }).then(res => {
    const token = _.get(res, 'body.token', null);
    if (!_.isNull(token)) {
      setUserKey(token);
    }
  });
});
