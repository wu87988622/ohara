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

describe('Root route', () => {
  it('should display root route', () => {
    cy.visit('/')
      .findByText(`You don't have any workspace yet!`)
      .should('exist');
  });
});

describe('Not found page', () => {
  it('should display page not found route', () => {
    // Another URL pattern, we can add more patterns here
    // to ensure different route patterns are all handled
    // properly
    cy.visit('/jladkf/safkj')
      .findByText('Ooooops, page not found!')
      .should('exist');
  });
});
