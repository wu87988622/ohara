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

import * as generate from '../../../src/utils/generate';

describe('Create Workspace', () => {
  // generate fake workspace name
  const workspaceName = generate.serviceName({ prefix: 'wk' });
  // generate fake node name
  const nodeHost = generate.serviceName({ prefix: 'node' });

  beforeEach(() => {
    cy.deleteAllServices();
  });

  context('Quick Create Workspace', () => {
    it('should remember the state when close the dialog', () => {
      cy.visit('/');
      // first visit will popup the quick create dialog
      cy.findByText('QUICK CREATE').click();

      // type workspace name
      cy.findByDisplayValue('workspace', { exact: false })
        .clear()
        .type(workspaceName);
      cy.findAllByText('NEXT').filter(':visible').click();

      // add node
      cy.contains('p:visible', 'Click here to select nodes').click();
      cy.findByTitle('Create Node').click();
      cy.get('input[name=hostname]').type(nodeHost);
      cy.get('input[name=port]').type(generate.port().toString());
      cy.get('input[name=user]').type(generate.userName());
      cy.get('input[name=password]').type(generate.password());
      cy.findByText('CREATE').click();
      cy.findByText(nodeHost)
        .siblings('td')
        .find('input[type="checkbox"]')
        .click();
      cy.findByText('SAVE').click();
      cy.findAllByText('NEXT').filter(':visible').click();

      // assert the node data should appear when click back button
      cy.findAllByText('BACK').filter(':visible').click();
      cy.contains('h6', 'Hostname')
        .siblings('div')
        .invoke('html')
        .should('equal', nodeHost);

      // close dialog
      cy.findByTestId('fullscreen-dialog-close-button').click();

      // back to create workspace dialog again
      // the state should keep in "select nodes"
      cy.findByText('QUICK CREATE').click();
      cy.contains('h6', 'Hostname')
        .siblings('div')
        .invoke('html')
        .should('equal', nodeHost);
    });

    it('should able to selected and filtered node', () => {
      cy.visit('/');
      cy.findByTestId('close-intro-button').click();
      cy.findByTitle('Node list').should('exist').click();

      const hostname1 = generate.serviceName();
      cy.findByTitle('Create Node').click();
      cy.get('input[name=hostname]').type(hostname1);
      cy.get('input[name=port]').type(generate.port().toString());
      cy.get('input[name=user]').type(generate.userName());
      cy.get('input[name=password]').type(generate.password());
      cy.findByText('CREATE').click();
      cy.findByText(hostname1).should('exist');

      cy.visit('/');
      cy.findByTestId('close-intro-button').click();
      cy.findByTitle('Node list').should('exist').click();

      const hostname2 = generate.serviceName();
      cy.findByTitle('Create Node').click();
      cy.get('input[name=hostname]').type(hostname2);
      cy.get('input[name=port]').type(generate.port().toString());
      cy.get('input[name=user]').type(generate.userName());
      cy.get('input[name=password]').type(generate.password());
      cy.findByText('CREATE').click();
      cy.findByText(hostname2).should('exist');

      cy.visit('/');
      cy.findByTestId('close-intro-button').click();
      cy.findByTitle('Node list').should('exist').click();

      const hostname3 = `${hostname1}${generate.serviceName()}`;
      cy.findByTitle('Create Node').click();
      cy.get('input[name=hostname]').type(hostname3);
      cy.get('input[name=port]').type(generate.port().toString());
      cy.get('input[name=user]').type(generate.userName());
      cy.get('input[name=password]').type(generate.password());
      cy.findByText('CREATE').click();
      cy.findByText(hostname3).should('exist');

      cy.visit('/');
      cy.findByText(/^quick create$/i)
        .should('exist')
        .click();

      // Step1: workspace name (using default)
      cy.findAllByText('NEXT').filter(':visible').click();

      // Since Unavailable node could not be selected
      // We check the existence only
      cy.findByText('Click here to select nodes').click();
      cy.findByText(hostname1).should('exist');
      cy.findByText(hostname2).should('exist');
      cy.findByText(hostname3).should('exist');

      // filter by hostname
      cy.findAllByPlaceholderText('Search').filter(':visible').type(hostname2);
      cy.findByText(hostname1).should('not.exist');
      cy.findByText(hostname3).should('not.exist');
    });

    it('should reset the form after create workspace successfully', () => {
      cy.createWorkspace({ workspaceName });

      // after creation with specific workspace name, the workspace should use default name
      cy.findByTitle('Create a new workspace').click();
      cy.findByText('QUICK CREATE').should('exist').click();
      cy.findByDisplayValue('workspace', { exact: false })
        .invoke('val')
        .should('equal', 'workspace1');

      cy.findAllByText('NEXT').filter(':visible').click();

      // the node selected cards should be initialized (only the "select nodes" card exists)
      cy.get('div.MuiGrid-container').children('div').should('have.length', 1);
    });
  });
});
