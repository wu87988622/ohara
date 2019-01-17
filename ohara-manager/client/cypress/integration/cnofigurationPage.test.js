/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as URLS from '../../src/constants/urls';

describe('configuration page', () => {
  beforeEach(() => {
    cy.visit(URLS.CONFIGURATION);
  });

  it('should go to configuration page', () => {
    cy.location('pathname').should('eq', URLS.CONFIGURATION);
  });

  it('should pass the connection test with correct HDFS info', () => {
    cy.getByLabelText('Name')
      .clear()
      .type('test connection');
    cy.getByLabelText('HDFS connection URL')
      .clear()
      .type('file://test/connection');

    cy.getByText('Test connection').click();

    cy.wait(3000);
    cy.get('.toast-success').should('have.length.above', 1);
  });

  it('should not pass the connection with wrong HDFS info', () => {
    cy.getByLabelText('Name')
      .clear()
      .type('test connection');
    cy.getByLabelText('HDFS connection URL')
      .clear()
      .type('somethingwrongaboutthisurl');

    cy.getByText('Test connection').click();
    cy.wait(3000);
    cy.get('.toast-error').should('have.length', 1);
  });
});
