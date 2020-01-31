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

import { deleteAllServices } from '../utils';

describe('Root route', () => {
  before(async () => await deleteAllServices());

  it('should display root route', () => {
    cy.visit('/')
      .location()
      .should(location => {
        expect(location.pathname).to.be.eq('/');
      })
      .findByText('QUICK START')
      .should('exist')
      .end();
  });
});

describe('Not found page', () => {
  it('should display page not found route', () => {
    // Another URL pattern, we can add more patterns here
    // to ensure different route patterns are all handled
    // properly
    cy.visit('/jladkf/safkj/ksjdl/jlkfsd/kjlfds')
      .contains('404')
      .should('exist');
  });
});
