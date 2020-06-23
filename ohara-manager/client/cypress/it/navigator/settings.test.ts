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

import { NodeRequest } from '../../../src/api/apiInterface/nodeInterface';
import * as generate from '../../../src/utils/generate';
import { SETTING_SECTIONS } from '../../support/customCommands';

describe('Navigator', () => {
  // generate fake node
  const node: NodeRequest = {
    hostname: generate.serviceName(),
    port: generate.port(),
    user: generate.userName(),
    password: generate.password(),
  };
  // generate topics
  const t1 = generate.serviceName({ prefix: 't1' });
  const t2 = generate.serviceName({ prefix: 't2' });

  // generate setting fillings
  const setting = generate.word();

  before(() => {
    cy.deleteAllServices();
    cy.createWorkspace({ node });
  });

  beforeEach(() => {
    // our tests should begin from home page
    cy.visit('/');
  });

  context('Settings', () => {
    it('should have navigator after adding a workspace', () => {
      // show workspace in header
      cy.get('#navigator').within(() => {
        // we should have a menu button of current workspace
        cy.contains('button', /workspace1/i);
        cy.get('#pipeline').within(() => {
          // should have no pipeline exists
          cy.get('ul li').should('not.exist');
        });
        cy.get('#outline').within(() => {
          // should have no outline exists
          cy.get('ul li').should('not.exist');
        });
      });
    });

    it('should able to click the workspace menu', () => {
      cy.get('#navigator').within(() => {
        cy.contains('button', /workspace1/i).click();
      });

      cy.findAllByRole('menu')
        .filter(':visible')
        .should('have.length', 1)
        .find('li')
        // we have only one menu, "Settings"
        .should('have.length', 1)
        .type('{ESC}');
    });

    it('should able to see settings of current workspace', () => {
      cy.get('#navigator').within(() => {
        cy.contains('button', /workspace1/i).click();
      });

      cy.findAllByRole('menu').filter(':visible').find('li').click();

      // assert all the sections
      cy.findAllByRole('dialog')
        .filter(':visible')
        .should('have.length', 1)
        .within(() => {
          cy.get('section').should(
            'have.length',
            Object.keys(SETTING_SECTIONS).length,
          );
          for (var section of Object.values(SETTING_SECTIONS)) {
            cy.contains('section', section);
          }
        });
    });
  });

  context('Topics Settings', () => {
    it('should have no topics initially', () => {
      cy.switchSettingSection(SETTING_SECTIONS.topics);

      // there should not have any topic initially
      cy.get('div.shared-topic').within(() => {
        cy.contains('tr', 'No records to display');
      });
      cy.get('div.pipeline-only-topic').within(() => {
        cy.contains('tr', 'No records to display');
      });
    });

    it('should able to add shared topics', () => {
      cy.switchSettingSection(SETTING_SECTIONS.topics);

      // add shared topics
      cy.findByTitle('Create Topic').should('be.enabled').click();

      cy.findAllByRole('dialog')
        .filter(':visible')
        .within(() => {
          cy.findAllByLabelText('Topic name', { exact: false })
            .filter(':visible')
            .type(t1);
          cy.findAllByLabelText('Partitions', { exact: false })
            .filter(':visible')
            .type('1');
          cy.findAllByLabelText('Replication factor', { exact: false })
            .filter(':visible')
            .type('1');
          cy.contains('button', /create/i).click();
        });

      // the new added topic should exist and running
      cy.findByText(t1).parent('tr').contains('td', 'RUNNING');

      cy.findByTitle('View topic').click();
      cy.findAllByRole('dialog')
        .filter(':visible')
        .within(() => {
          // assert topic name
          cy.contains('td', 'Name')
            .siblings('td')
            .invoke('html')
            .should('equal', t1);
          // this topic should be shared
          cy.contains('td', 'Type').siblings('td').contains('span', 'Shared');
        })
        // press "ESC" again back to topic list
        .trigger('keydown', { keyCode: 27, which: 27 });

      // delete topic
      cy.findByTitle('Delete topic').click();
      // cancel deletion
      cy.findAllByRole('dialog')
        .filter(':visible')
        .within(() => {
          cy.contains('button', /cancel/i).click();
        });
      cy.findByTitle('Delete topic').click();
      cy.findAllByRole('dialog')
        .filter(':visible')
        .within(() => {
          cy.contains('button', /delete/i).click();
        });
      // the topic should not exist
      cy.findByText(t1).should('not.exist');
    });

    it('should able to filter and sort topics by name', () => {
      cy.switchSettingSection(SETTING_SECTIONS.topics);

      // add multiple topics
      cy.findByTitle('Create Topic').should('be.enabled').click();
      cy.findAllByRole('dialog')
        .filter(':visible')
        .within(() => {
          cy.findAllByLabelText('Topic name', { exact: false })
            .filter(':visible')
            .type(t1);
          cy.findAllByLabelText('Partitions', { exact: false })
            .filter(':visible')
            .type('1');
          cy.findAllByLabelText('Replication factor', { exact: false })
            .filter(':visible')
            .type('1');
          cy.contains('button', /create/i).click();
        });
      cy.findByTitle('Create Topic').should('be.enabled').click();
      cy.findAllByRole('dialog')
        .filter(':visible')
        .within(() => {
          cy.findAllByLabelText('Topic name', { exact: false })
            .filter(':visible')
            .type(t2);
          cy.findAllByLabelText('Partitions', { exact: false })
            .filter(':visible')
            .type('1');
          cy.findAllByLabelText('Replication factor', { exact: false })
            .filter(':visible')
            .type('1');
          cy.contains('button', /create/i).click();
        });
      // should able to filter topics by name
      cy.get('div.shared-topic').within(() => {
        cy.findAllByPlaceholderText('Search').filter(':visible').type(t2);
        cy.findByText(t1).should('not.exist');
        cy.findByText(t2).should('exist');
        cy.findAllByPlaceholderText('Search').filter(':visible').clear();

        // sort the topics by name
        cy.get('thead tr th').first().find('svg').dblclick();

        // the t2 topic should be first element in reverse order
        cy.get('tbody tr').first().contains('td', t2);
      });

      cy.findAllByTitle('Delete topic').first().click();
      cy.findAllByRole('dialog')
        .filter(':visible')
        .within(() => {
          cy.contains('button', /delete/i).click();
        });

      // the t2 topic should not exist
      cy.findByText(t2).should('not.exist');

      cy.findByTitle('Delete topic').click();
      cy.findAllByRole('dialog')
        .filter(':visible')
        .within(() => {
          cy.contains('button', /delete/i).click();
        });

      // the t1 topic should not exist
      cy.findByText(t1).should('not.exist');

      // click back arrow button to settings page
      cy.findAllByText('Topics in this workspace')
        .filter(':visible')
        .siblings('button')
        .click();
    });

    it('validation of topic creation', () => {
      cy.switchSettingSection(SETTING_SECTIONS.topics);

      cy.findByTitle('Create Topic').should('be.enabled').click();
      cy.findAllByRole('dialog')
        .filter(':visible')
        .within(() => {
          cy.findAllByLabelText('Topic name', { exact: false })
            .filter(':visible')
            .type(t1);
          // only accept positive partitions
          cy.findAllByLabelText('Partitions', { exact: false })
            .filter(':visible')
            .type('0')
            .blur();
          cy.findByText('Partitions must be greater than or equal to 1').should(
            'exist',
          );
          // only accept positive replication factor
          cy.findAllByLabelText('Replication factor', { exact: false })
            .filter(':visible')
            .type('0')
            .blur();
          cy.findByText(
            'Replication factor must be greater than or equal to 1',
          ).should('exist');
          // replication factor could not bigger than node size
          cy.findAllByLabelText('Replication factor', { exact: false })
            .filter(':visible')
            .clear()
            .type('2');
          cy.findByText(
            'Replication factor should be less than or equal to 1 broker as there is only 1 available in the current workspace',
          ).should('exist');
          cy.contains('button', /cancel/i).click();
        });
    });
  });

  context('Autofill Settings', () => {
    it('should have no data initially', () => {
      cy.switchSettingSection(SETTING_SECTIONS.autofill);

      // there should not have any setting filling initially
      cy.get('div.section-page-content').within(() => {
        cy.get('ul li').should('not.exist');
      });
    });

    it('should able to add setting fillings', () => {
      cy.switchSettingSection(SETTING_SECTIONS.autofill);

      cy.contains('button', /add autofill/i).click();

      // add a setting filling
      cy.get('input[name="displayName"]').type(setting);

      cy.contains('button', /add key/i).click();
      cy.get('input[name="settings[0].key"]').type('completed');
      cy.findByText('completed.folder').click();
      cy.get('input[name="settings[0].value"]').type('fake');

      cy.contains('button', /add key/i).click();
      cy.get('input[name="settings[1].key"]').type('ftp.p');
      cy.findByText('ftp.port').click();
      cy.get('input[name="settings[1].value"]').type('22');

      cy.contains('button', /add key/i).click();
      cy.get('input[name="settings[2].key"]').type('error');
      cy.findByText('error.folder').click();

      // remove the last one key-value pair
      cy.findAllByTitle('Delete the set of key, value')
        .filter(':visible')
        .last()
        .click();

      // click save button
      cy.contains('button', /save/i).click();
      cy.findByText(setting).should('exist');

      // edit this setting
      cy.findAllByTitle('Edit the autofill').filter(':visible').click();
      // could change name and add new key
      cy.get('input[name="displayName"]').clear().type('foo');
      cy.contains('button', /add key/i).click();
      cy.get('input[name="settings[2].key"]').type('ftp.ho');
      cy.findByText('ftp.hostname').click();

      // click save button
      cy.contains('button', /save/i).click();
      // the origin setting name should not exist
      cy.findByText(setting).should('not.exist');

      // copy this setting
      cy.findAllByTitle('Copy the autofill').filter(':visible').click();
      cy.get('input[name="displayName"]').clear().type(setting);
      cy.contains('button', /save/i).click();

      // we have two settings now
      cy.get('div.section-page-content').within(() => {
        cy.get('ul li').should('have.length', 2);
      });

      // remove all settings
      cy.findAllByTitle('Delete the autofill')
        .filter(':visible')
        .each(($el) => {
          cy.wrap($el).click();
          cy.contains('button', /delete/i)
            .filter(':visible')
            .click();
        });
    });

    it('could not add duplicate name or key of setting fillings', () => {
      cy.switchSettingSection(SETTING_SECTIONS.autofill);

      // add a setting filling
      cy.contains('button', /add autofill/i).click();
      cy.get('input[name="displayName"]').type(setting);

      cy.contains('button', /add key/i).click();
      cy.get('input[name="settings[0].key"]').type('completed');
      cy.findByText('completed.folder').click();
      cy.get('input[name="settings[0].value"]').type('fake');

      cy.contains('button', /add key/i).click();
      cy.get('input[name="settings[1].key"]').type('completed');
      cy.findByText('completed.folder').click();
      // key could not duplicated
      cy.findByText('Key is duplicated').should('exist');

      // remove the last one key-value pair
      cy.findAllByTitle('Delete the set of key, value')
        .filter(':visible')
        .last()
        .click();

      // click save button
      cy.contains('button', /save/i).click();
      cy.findByText(setting).should('exist');

      // add a same name setting filling
      cy.contains('button', /add autofill/i).click();
      cy.get('input[name="displayName"]').type(setting).blur();
      cy.findByText(
        `Name '${setting}' already existed. Please use a different name`,
      ).should('exist');

      // click cancel button
      cy.contains('button', /cancel/i).click();

      // remove setting
      cy.findAllByTitle('Delete the autofill')
        .filter(':visible')
        .each(($el) => {
          cy.wrap($el).click();
          // click cancel deletion
          cy.contains('button', /cancel/i)
            .filter(':visible')
            .click();
          // remove setting again
          cy.wrap($el).click();
          cy.contains('button', /delete/i)
            .filter(':visible')
            .click();
        });
    });
  });

  context('Zookeeper Settings', () => {
    it('should have one node in zookeeper after creating workspace', () => {
      cy.switchSettingSection(SETTING_SECTIONS.zookeeper);

      // there should have one zookeeper node
      cy.get('div.section-page-content').within(() => {
        cy.get('tbody tr').should('have.length', 1);
        cy.get('tbody tr').contains('td', node.hostname);
      });
    });

    it('should able to view node information of zookeeper', () => {
      cy.switchSettingSection(SETTING_SECTIONS.zookeeper);

      cy.findAllByTitle('View node').filter(':visible').click();

      cy.findAllByRole('dialog')
        .filter(':visible')
        .within(() => {
          // assert each field
          cy.contains('td', 'Hostname')
            .siblings('td')
            .invoke('html')
            .should('equal', node.hostname);
          cy.contains('td', 'Port')
            .siblings('td')
            .invoke('html')
            .should('equal', node.port.toString());
          cy.contains('td', 'User')
            .siblings('td')
            .invoke('html')
            .should('equal', node.user);
          cy.contains('td', 'Password')
            .siblings('td')
            .invoke('html')
            .should('equal', node.password);

          // assert zookeeper should exist in services
          cy.contains('td', 'zookeeper')
            .siblings('td')
            .first()
            .invoke('html')
            // the name of zookeeper should be as same as "workspace"
            .should('equal', 'workspace1');
        });
    });
  });

  context('Broker Settings', () => {
    it('should have one node in broker after creating workspace', () => {
      cy.switchSettingSection(SETTING_SECTIONS.broker);

      // there should have one zookeeper node
      cy.get('div.section-page-content').within(() => {
        cy.get('tbody tr').should('have.length', 1);
        cy.get('tbody tr').contains('td', node.hostname);
      });
    });

    it('should able to view node information of broker', () => {
      cy.switchSettingSection(SETTING_SECTIONS.broker);

      cy.findAllByTitle('View node').filter(':visible').click();

      cy.findAllByRole('dialog')
        .filter(':visible')
        .within(() => {
          // assert each field
          cy.contains('td', 'Hostname')
            .siblings('td')
            .invoke('html')
            .should('equal', node.hostname);
          cy.contains('td', 'Port')
            .siblings('td')
            .invoke('html')
            .should('equal', node.port.toString());
          cy.contains('td', 'User')
            .siblings('td')
            .invoke('html')
            .should('equal', node.user);
          cy.contains('td', 'Password')
            .siblings('td')
            .invoke('html')
            .should('equal', node.password);

          // assert broker should exist in services
          cy.contains('td', 'worker')
            .siblings('td')
            .first()
            .invoke('html')
            // the name of broker should be as same as "workspace"
            .should('equal', 'workspace1');
        });
    });
  });

  context('Worker Settings', () => {
    it('should have one node in worker after creating workspace', () => {
      cy.switchSettingSection(SETTING_SECTIONS.worker);

      // there should have one zookeeper node
      cy.get('div.section-page-content').within(() => {
        cy.get('tbody tr').should('have.length', 1);
        cy.get('tbody tr').contains('td', node.hostname);
      });
    });

    it('should able to view node information of worker', () => {
      cy.switchSettingSection(SETTING_SECTIONS.worker);

      cy.findAllByTitle('View node').filter(':visible').click();

      cy.findAllByRole('dialog')
        .filter(':visible')
        .within(() => {
          // assert each field
          cy.contains('td', 'Hostname')
            .siblings('td')
            .invoke('html')
            .should('equal', node.hostname);
          cy.contains('td', 'Port')
            .siblings('td')
            .invoke('html')
            .should('equal', node.port.toString());
          cy.contains('td', 'User')
            .siblings('td')
            .invoke('html')
            .should('equal', node.user);
          cy.contains('td', 'Password')
            .siblings('td')
            .invoke('html')
            .should('equal', node.password);

          // assert worker should exist in services
          cy.contains('td', 'worker')
            .siblings('td')
            .first()
            .invoke('html')
            // the name of worker should be as same as "workspace"
            .should('equal', 'workspace1');
        });
    });
  });

  context('Nodes Settings', () => {
    it('should have one node in workspace', () => {
      cy.switchSettingSection(SETTING_SECTIONS.nodes);

      // there should have one node
      cy.get('div.section-page-content').within(() => {
        cy.get('tbody tr').should('have.length', 1);
        cy.get('tbody tr').contains('td', node.hostname);
        cy.get('tbody tr')
          .children('td')
          // the "Used" column
          .eq(4)
          .invoke('html')
          .should(
            'equal',
            '<div>Broker</div><div>Worker</div><div>Zookeeper</div>',
          );
      });
    });
  });

  context('Combine restart and delete workspaces operations', () => {
    it('should able to restart the same name workspace which just removed and re-created', () => {
      cy.switchSettingSection(
        SETTING_SECTIONS.dangerZone,
        'Delete this workspace',
      );
      cy.findAllByRole('dialog')
        .filter(':visible')
        .should('have.length', 1)
        .within(() => {
          cy.get('input').type('workspace1');
          cy.findByText('DELETE').click();
        });

      // after removed, the quick create should be appear
      cy.findByTestId('close-intro-button').should('be.visible');

      cy.createWorkspace({ node });

      // click restart workspace should be OK
      cy.switchSettingSection(
        SETTING_SECTIONS.dangerZone,
        'Restart this workspace',
      );

      cy.findAllByRole('dialog')
        .filter(':visible')
        .should('have.length', 1)
        .within(() => {
          cy.findByText('RESTART').click();
        });
      cy.findAllByRole('dialog')
        .filter(':visible')
        .should('have.length', 1)
        .within(() => {
          cy.findByText('CLOSE').parent('button').should('be.enabled').click();
        });

      cy.location('pathname').should('equal', '/workspace1');
    });
  });
});
