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
import { SETTING_SECTION } from '../../support/customCommands';
import { hashByGroupAndName } from '../../../src/utils/sha';

describe('Danger Zone', () => {
  // generate fake node
  const node: NodeRequest = {
    hostname: generate.serviceName(),
    port: generate.port(),
    user: generate.userName(),
    password: generate.password(),
  };

  const hostname = generate.serviceName();

  before(() => {
    cy.deleteAllServices();
    cy.createWorkspace({ node });
  });

  beforeEach(() => {
    // our tests should begin from home page
    cy.visit('/');
    cy.server();
  });

  it('should able to add node into workspace', () => {
    // click node list
    cy.findByTitle(/node list/i).click();
    // create a node
    cy.findByTitle(/create node/i).click();
    cy.get('input[name=hostname]').type(hostname);
    cy.get('input[name=port]').type(generate.port().toString());
    cy.get('input[name=user]').type(generate.userName());
    cy.get('input[name=password]').type(generate.password());
    cy.findByText('CREATE').click();
    cy.findByTestId('nodes-dialog-close-button').click();

    // add newly added node into workspace
    cy.switchSettingSection(SETTING_SECTION.nodes);
    cy.get('div.section-page-content').within(() => {
      cy.findByTitle('Add Node').click();
    });

    // reopen select node
    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => cy.findByText('CANCEL').click());
    cy.get('div.section-page-content').within(() => {
      cy.findByTitle('Add Node').click();
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.get('table')
          .should('have.length', 1)
          .within(($table) => {
            // check the newly added hostname
            cy.getTableCellByColumn($table, 'Hostname', hostname)
              .should('exist')
              .siblings('td')
              .first()
              .find('input[type="checkbox"]')
              .click();
          });

        cy.findByText('SAVE').click();
      });

    cy.get('div.section-page-content').within(() => {
      cy.findByText(hostname)
        .should('exist')
        .siblings('td')
        // the "Used" column
        .eq(3)
        .invoke('html')
        // there is no service assigned to this node yet
        .should('be.empty');
    });
  });

  it('should show an restart indicator after adding node to zookeeper', () => {
    cy.switchSettingSection(SETTING_SECTION.zookeeper);

    cy.get('div.section-page-content').within(() => {
      cy.findByTitle('Add Node').click();
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.get('table')
          .should('have.length', 1)
          .within(($table) => {
            // assert the Services should be 0
            cy.getTableCellByColumn($table, 'Services', '0').should('exist');
            // check the newly added hostname
            cy.getTableCellByColumn($table, 'Name', hostname)
              .should('exist')
              .siblings('td')
              .first()
              .find('input[type="checkbox"]')
              .click();
          });

        cy.findByText('SAVE').click();
      });

    cy.get('div.section-page-content').within(() => {
      // undo added node
      cy.findByText(hostname)
        .siblings('td')
        .last()
        .within(() => cy.findByTitle('Undo add node').click());
      cy.findByText(hostname).should('not.exist');
    });

    // adding node again
    cy.get('div.section-page-content').within(() => {
      cy.findByTitle('Add Node').click();
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.get('table')
          .should('have.length', 1)
          .within(($table) => {
            // assert the Services should be 0
            cy.getTableCellByColumn($table, 'Services', '0').should('exist');
            // check the newly added hostname
            cy.getTableCellByColumn($table, 'Name', hostname)
              .should('exist')
              .siblings('td')
              .first()
              .find('input[type="checkbox"]')
              .click();
          });

        cy.findByText('SAVE').click();
      });

    cy.get('div.section-page-header').within(() => {
      // back to Settings dialog
      cy.get('button').click();
    });

    // the zookeeper section should have 1 change warning
    cy.contains('h2', SETTING_SECTION.zookeeper)
      .parent('section')
      .find('ul')
      .contains('span', '1');

    // click the discard button in indicator
    cy.findAllByRole('alert')
      .scrollIntoView()
      .should('have.length', 1)
      .within(() => {
        cy.contains('button', 'DISCARD').click();
      });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        // discard the "discard" changes
        cy.contains('button', 'CANCEL').click();
      });

    // click the discard button in indicator again
    cy.findAllByRole('alert')
      .scrollIntoView()
      .should('have.length', 1)
      .within(() => {
        cy.contains('button', 'DISCARD').click();
      });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        // discard the changes
        cy.contains('button', 'DISCARD').click();
      });

    // the zookeeper section should not have warnings
    cy.findAllByRole('alert').should('not.exist');
    cy.contains('h2', SETTING_SECTION.zookeeper)
      .parent('section')
      .find('ul')
      .contains('span', '1')
      .should('not.exist');
  });

  it('should able to restart from indicator after adding node to zookeeper', () => {
    // add node to zookeeper
    cy.switchSettingSection(SETTING_SECTION.zookeeper);
    cy.get('div.section-page-content').within(() => {
      cy.findByTitle('Add Node').click();
    });
    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.get('table')
          .should('have.length', 1)
          .within(($table) => {
            // check the newly added hostname
            cy.getTableCellByColumn($table, 'Name', hostname)
              .should('exist')
              .siblings('td')
              .first()
              .find('input[type="checkbox"]')
              .click();
          });
        cy.findByText('SAVE').click();
      });
    cy.get('div.section-page-header').within(() => {
      // back to Settings dialog
      cy.get('button').click();
    });

    // click the restart button in indicator
    cy.findAllByRole('alert')
      .scrollIntoView()
      .should('have.length', 1)
      .within(() => {
        cy.contains('button', 'RESTART').click();
      });
    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        // cancel the "restart" changes
        cy.contains('button', 'CANCEL').click();
      });

    // click the restart button in indicator again
    cy.findAllByRole('alert')
      .scrollIntoView()
      .should('have.length', 1)
      .within(() => {
        cy.contains('button', 'RESTART').click();
      });
    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        // confirm the "restart" changes
        cy.contains('button', 'RESTART').click();
      });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        // expand the log process
        cy.findByText('Stop worker', { exact: false });
        cy.findByText('update worker', { exact: false });
        cy.findByText('Stop topic', { exact: false });
        cy.findByText('Stop broker', { exact: false });
        cy.findByText('update broker', { exact: false });
        cy.findByText('Stop zookeeper', { exact: false });
        cy.findByText('update zookeeper', { exact: false });
        cy.findByText('Start zookeeper', { exact: false });
        cy.findByText('Start broker', { exact: false });
        cy.findByText('Start topic', { exact: false });
        cy.findByText('Start worker', { exact: false });
        cy.findByText('Restart workspace', { exact: false });

        cy.findByText('CLOSE').parent('button').should('be.enabled').click();
      });

    // close the snackbar
    cy.findByTestId('snackbar').find('button:visible').click();

    // close the settings dialog
    cy.findByTestId('workspace-settings-dialog-close-button')
      .should('be.visible')
      .click();

    cy.switchSettingSection(SETTING_SECTION.zookeeper);
    cy.get('div.section-page-content').within(() => {
      cy.get('table')
        .should('have.length', 1)
        .within(($table) => {
          // assert the Services should be 1 now
          cy.getTableCellByColumn($table, 'Services', '1').should('exist');
        });
    });
  });

  it('should able to restart from indicator after adding node to broker', () => {
    // add node to zookeeper
    cy.switchSettingSection(SETTING_SECTION.broker);
    cy.get('div.section-page-content').within(() => {
      cy.findByTitle('Add Node').click();
    });
    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.get('table')
          .should('have.length', 1)
          .within(($table) => {
            // check the newly added hostname
            cy.getTableCellByColumn($table, 'Name', hostname)
              .should('exist')
              .siblings('td')
              .first()
              .find('input[type="checkbox"]')
              .click();
          });
        cy.findByText('SAVE').click();
      });
    cy.get('div.section-page-header').within(() => {
      // back to Settings dialog
      cy.get('button').click();
    });

    // click the restart button in indicator
    cy.findAllByRole('alert')
      .scrollIntoView()
      .should('have.length', 1)
      .within(() => {
        cy.contains('button', 'RESTART').click();
      });
    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        // cancel the "restart" changes
        cy.contains('button', 'CANCEL').click();
      });

    // click the restart button in indicator again
    cy.findAllByRole('alert')
      .scrollIntoView()
      .should('have.length', 1)
      .within(() => {
        cy.contains('button', 'RESTART').click();
      });
    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        // confirm the "restart" changes
        cy.contains('button', 'RESTART').click();
      });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        // expand the log process
        cy.findByText('Stop worker', { exact: false });
        cy.findByText('update worker', { exact: false });
        cy.findByText('Stop topic', { exact: false });
        cy.findByText('Stop broker', { exact: false });
        cy.findByText('update broker', { exact: false });
        cy.findByText('Start broker', { exact: false });
        cy.findByText('Start topic', { exact: false });
        cy.findByText('Start worker', { exact: false });
        cy.findByText('Restart workspace', { exact: false });

        cy.findByText('CLOSE').parent('button').should('be.enabled').click();
      });

    // close the snackbar
    cy.findByTestId('snackbar').find('button:visible').click();

    cy.switchSettingSection(SETTING_SECTION.zookeeper);
    cy.get('div.section-page-content').within(() => {
      cy.get('table')
        .should('have.length', 1)
        .within(($table) => {
          // assert the Services should be 1 now
          cy.getTableCellByColumn($table, 'Services', '1').should('exist');
        });
    });
  });

  it('should able to restart from indicator after adding node to worker', () => {
    // add node to zookeeper
    cy.switchSettingSection(SETTING_SECTION.worker);
    cy.get('div.section-page-content').within(() => {
      cy.findByTitle('Add Node').click();
    });
    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.get('table')
          .should('have.length', 1)
          .within(($table) => {
            // check the newly added hostname
            cy.getTableCellByColumn($table, 'Name', hostname)
              .should('exist')
              .siblings('td')
              .first()
              .find('input[type="checkbox"]')
              .click();
          });
        cy.findByText('SAVE').click();
      });
    cy.get('div.section-page-header').within(() => {
      // back to Settings dialog
      cy.get('button').click();
    });

    // click the restart button in indicator
    cy.findAllByRole('alert')
      .scrollIntoView()
      .should('have.length', 1)
      .within(() => {
        cy.contains('button', 'RESTART').click();
      });
    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        // cancel the "restart" changes
        cy.contains('button', 'CANCEL').click();
      });

    // click the restart button in indicator again
    cy.findAllByRole('alert')
      .scrollIntoView()
      .should('have.length', 1)
      .within(() => {
        cy.contains('button', 'RESTART').click();
      });
    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        // confirm the "restart" changes
        cy.contains('button', 'RESTART').click();
      });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        // expand the log process
        cy.findByText('Stop worker', { exact: false });
        cy.findByText('update worker', { exact: false });
        cy.findByText('Start worker', { exact: false });
        cy.findByText('Restart workspace', { exact: false });

        cy.findByText('CLOSE').parent('button').should('be.enabled').click();
      });

    // close the snackbar
    cy.findByTestId('snackbar').find('button:visible').click();

    // close the settings dialog
    cy.findByTestId('workspace-settings-dialog-close-button')
      .should('be.visible')
      .click();

    cy.switchSettingSection(SETTING_SECTION.zookeeper);
    cy.get('div.section-page-content').within(() => {
      cy.get('table')
        .should('have.length', 1)
        .within(($table) => {
          // assert the Services should be 1 now
          cy.getTableCellByColumn($table, 'Services', '1').should('exist');
        });
    });
  });

  it('should able to restart worker by directly click button', () => {
    cy.switchSettingSection(SETTING_SECTION.dangerZone, 'Restart this worker');
    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('CANCEL').click();
      });

    // click again
    cy.switchSettingSection(SETTING_SECTION.dangerZone, 'Restart this worker');

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RESTART').click();
      });

    cy.findByText('Stop worker', { exact: false });
    cy.findByText('update worker', { exact: false });
    cy.findByText('Start worker', { exact: false });
    cy.findByText('Restart workspace', { exact: false });

    cy.findByText('CLOSE').parent('button').should('be.enabled').click();
  });

  it('should able to restart broker by directly click button', () => {
    cy.switchSettingSection(SETTING_SECTION.dangerZone, 'Restart this broker');
    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('CANCEL').click();
      });

    // click again
    cy.switchSettingSection(SETTING_SECTION.dangerZone, 'Restart this broker');

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RESTART').click();
      });

    cy.findByText('Stop worker', { exact: false });
    cy.findByText('update worker', { exact: false });
    cy.findByText('Stop topic', { exact: false });
    cy.findByText('Stop broker', { exact: false });
    cy.findByText('update broker', { exact: false });
    cy.findByText('Start broker', { exact: false });
    cy.findByText('Start topic', { exact: false });
    cy.findByText('Start worker', { exact: false });
    cy.findByText('Restart workspace', { exact: false });

    cy.findByText('CLOSE').parent('button').should('be.enabled').click();
  });

  it('should able to restart workspace by directly click button', () => {
    cy.switchSettingSection(
      SETTING_SECTION.dangerZone,
      'Restart this workspace',
    );
    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('CANCEL').click();
      });

    // click again
    cy.switchSettingSection(
      SETTING_SECTION.dangerZone,
      'Restart this workspace',
    );

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RESTART').click();
      });

    cy.findByText('Stop worker', { exact: false });
    cy.findByText('update worker', { exact: false });
    cy.findByText('Stop topic', { exact: false });
    cy.findByText('Stop broker', { exact: false });
    cy.findByText('update broker', { exact: false });
    cy.findByText('Stop zookeeper', { exact: false });
    cy.findByText('update zookeeper', { exact: false });
    cy.findByText('Start zookeeper', { exact: false });
    cy.findByText('Start broker', { exact: false });
    cy.findByText('Start topic', { exact: false });
    cy.findByText('Start worker', { exact: false });
    cy.findByText('Restart workspace', { exact: false });

    cy.findByText('CLOSE').parent('button').should('be.enabled').click();
  });

  it('retry stop worker should be used normally in restart workspace', () => {
    cy.switchSettingSection(
      SETTING_SECTION.dangerZone,
      'Restart this workspace',
    );

    cy.route({
      method: 'PUT',
      url: 'api/workers/*/stop**',
      status: 403,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RESTART').click();
      });

    cy.findByText('ERROR', { exact: false });

    cy.request('PUT', 'api/workers/workspace1/stop?group=worker');

    cy.route({
      method: 'PUT',
      url: 'api/workers/*/stop**',
      status: 202,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RETRY').click();
      });

    cy.findByTestId('snackbar').should(
      'have.text',
      'Successfully Restart workspace workspace1.',
    );

    cy.findAllByText('Stop worker', { exact: false })
      .eq(1)
      .next()
      .should('have.text', '[OK]');
    cy.findByText('update worker', { exact: false })
      .next()
      .should('have.text', '[OK]');
    cy.findByText('Stop topic', { exact: false })
      .next()
      .should('have.text', '[OK]');
    cy.findByText('Stop broker', { exact: false })
      .next()
      .should('have.text', '[OK]');
    cy.findByText('update broker', { exact: false })
      .next()
      .should('have.text', '[OK]');
    cy.findByText('Stop zookeeper', { exact: false })
      .next()
      .should('have.text', '[OK]');
    cy.findByText('update zookeeper', { exact: false })
      .next()
      .should('have.text', '[OK]');
    cy.findByText('Start zookeeper', { exact: false })
      .next()
      .should('have.text', '[OK]');
    cy.findByText('Start broker', { exact: false })
      .next()
      .should('have.text', '[OK]');
    cy.findByText('Start topic', { exact: false })
      .next()
      .should('have.text', '[OK]');
    cy.findByText('Start worker', { exact: false })
      .next()
      .should('have.text', '[OK]');
    cy.findAllByText('Restart workspace', { exact: false })
      .eq(2)
      .next()
      .should('have.text', '[OK]');

    cy.findByText('CLOSE').parent('button').should('be.enabled').click();
  });

  it('retry update worker should be used normally in restart workspace', () => {
    cy.switchSettingSection(
      SETTING_SECTION.dangerZone,
      'Restart this workspace',
    );

    cy.route({
      method: 'PUT',
      url: 'api/workers/workspace1?group=worker',
      status: 403,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RESTART').click();
      });

    cy.findByText('ERROR', { exact: false });

    cy.route({
      method: 'PUT',
      url: 'api/workers/workspace1?group=worker',
      status: 200,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RETRY').click();
      });

    cy.findByTestId('snackbar').should(
      'have.text',
      'Successfully Restart workspace workspace1.',
    );

    cy.findByText('100%');

    cy.findByText('CLOSE').parent('button').should('be.enabled').click();
  });

  it('retry stop topic should be used normally in restart workspace', () => {
    cy.switchSettingSection(
      SETTING_SECTION.dangerZone,
      'Restart this workspace',
    );

    cy.route({
      method: 'GET',
      url: `api/topics?group=${hashByGroupAndName('workspace', 'workspace1')}`,

      status: 200,
      response: [
        { name: 't1', group: hashByGroupAndName('workspace', 'workspace1') },
        { name: 't2', group: hashByGroupAndName('workspace', 'workspace1') },
      ],
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RESTART').click();
      });

    cy.findByText('ERROR', { exact: false });

    cy.route({
      method: 'GET',
      url: `api/topics?group=${hashByGroupAndName('workspace', 'workspace1')}`,
      status: 200,
      response: [],
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RETRY').click();
      });

    cy.findByTestId('snackbar').should(
      'have.text',
      'Successfully Restart workspace workspace1.',
    );

    cy.findByText('100%');

    cy.findByText('CLOSE').parent('button').should('be.enabled').click();
  });

  it('retry stop broker should be used normally in restart workspace', () => {
    cy.switchSettingSection(
      SETTING_SECTION.dangerZone,
      'Restart this workspace',
    );

    cy.route({
      method: 'PUT',
      url: 'api/brokers/*/stop**',
      status: 403,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RESTART').click();
      });

    cy.findByText('ERROR', { exact: false });

    cy.request('PUT', 'api/brokers/workspace1/stop?group=broker');

    cy.route({
      method: 'PUT',
      url: 'api/brokers/*/stop**',
      status: 202,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RETRY').click();
      });

    cy.findByTestId('snackbar').should(
      'have.text',
      'Successfully Restart workspace workspace1.',
    );

    cy.findByText('100%');

    cy.findByText('CLOSE').parent('button').should('be.enabled').click();
  });

  it('retry update broker should be used normally in restart workspace', () => {
    cy.switchSettingSection(
      SETTING_SECTION.dangerZone,
      'Restart this workspace',
    );

    cy.route({
      method: 'PUT',
      url: 'api/brokers/workspace1?group=broker',
      status: 403,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RESTART').click();
      });

    cy.findByText('ERROR', { exact: false });

    cy.route({
      method: 'PUT',
      url: 'api/brokers/workspace1?group=broker',
      status: 200,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RETRY').click();
      });

    cy.findByTestId('snackbar').should(
      'have.text',
      'Successfully Restart workspace workspace1.',
    );

    cy.findByText('100%');

    cy.findByText('CLOSE').parent('button').should('be.enabled').click();
  });

  it('retry stop zookeeper should be used normally in restart workspace', () => {
    cy.switchSettingSection(
      SETTING_SECTION.dangerZone,
      'Restart this workspace',
    );

    cy.route({
      method: 'PUT',
      url: 'api/zookeepers/*/stop**',
      status: 403,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RESTART').click();
      });

    cy.findByText('ERROR', { exact: false });

    cy.request('PUT', 'api/zookeepers/workspace1/stop?group=zookeeper');

    cy.route({
      method: 'PUT',
      url: 'api/zookeepers/*/stop**',
      status: 202,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RETRY').click();
      });

    cy.findByTestId('snackbar').should(
      'have.text',
      'Successfully Restart workspace workspace1.',
    );

    cy.findByText('100%');

    cy.findByText('CLOSE').parent('button').should('be.enabled').click();
  });

  it('retry update zookeeper should be used normally in restart workspace', () => {
    cy.switchSettingSection(
      SETTING_SECTION.dangerZone,
      'Restart this workspace',
    );

    cy.route({
      method: 'PUT',
      url: 'api/zookeepers/workspace1?group=zookeeper',
      status: 403,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RESTART').click();
      });

    cy.findByText('ERROR', { exact: false });

    cy.route({
      method: 'PUT',
      url: 'api/zookeepers/workspace1?group=zookeeper',
      status: 200,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RETRY').click();
      });

    cy.findByTestId('snackbar').should(
      'have.text',
      'Successfully Restart workspace workspace1.',
    );

    cy.findByText('100%');

    cy.findByText('CLOSE').parent('button').should('be.enabled').click();
  });

  it('retry start zookeeper should be used normally in restart workspace', () => {
    cy.switchSettingSection(
      SETTING_SECTION.dangerZone,
      'Restart this workspace',
    );

    cy.route({
      method: 'PUT',
      url: 'api/zookeepers/*/start**',
      status: 403,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RESTART').click();
      });

    cy.findByText('ERROR', { exact: false });

    cy.request('PUT', 'api/zookeepers/workspace1/start?group=zookeeper');

    cy.route({
      method: 'PUT',
      url: 'api/zookeepers/*/start**',
      status: 202,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RETRY').click();
      });

    cy.findByTestId('snackbar').should(
      'have.text',
      'Successfully Restart workspace workspace1.',
    );

    cy.findByText('100%');

    cy.findByText('CLOSE').parent('button').should('be.enabled').click();
  });

  it('retry start broker should be used normally in restart workspace', () => {
    cy.switchSettingSection(
      SETTING_SECTION.dangerZone,
      'Restart this workspace',
    );

    cy.route({
      method: 'PUT',
      url: 'api/brokers/*/start**',
      status: 403,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RESTART').click();
      });

    cy.findByText('ERROR', { exact: false });

    cy.request('PUT', 'api/brokers/workspace1/start?group=broker');

    cy.route({
      method: 'PUT',
      url: 'api/brokers/*/start**',
      status: 202,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RETRY').click();
      });

    cy.findByTestId('snackbar').should(
      'have.text',
      'Successfully Restart workspace workspace1.',
    );

    cy.findByText('100%');

    cy.findByText('CLOSE').parent('button').should('be.enabled').click();
  });

  it('retry start topic should be used normally in restart workspace', () => {
    cy.switchSettingSection(
      SETTING_SECTION.dangerZone,
      'Restart this workspace',
    );

    cy.route({
      method: 'GET',
      url: `api/topics?group=${hashByGroupAndName('workspace', 'workspace1')}`,

      status: 200,
      response: [
        { name: 't1', group: hashByGroupAndName('workspace', 'workspace1') },
        { name: 't2', group: hashByGroupAndName('workspace', 'workspace1') },
      ],
    });

    cy.route({
      method: 'PUT',
      url: 'api/topics/*/stop**',
      status: 202,
      response: {},
    });

    cy.route({
      method: 'GET',
      url: `api/topics/t1?group=${hashByGroupAndName(
        'workspace',
        'workspace1',
      )}`,
      status: 200,
      response: {},
    });

    cy.route({
      method: 'GET',
      url: `api/topics/t2?group=${hashByGroupAndName(
        'workspace',
        'workspace1',
      )}`,
      status: 200,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RESTART').click();
      });

    cy.findByText('ERROR', { exact: false });

    cy.route({
      method: 'GET',
      url: `api/topics?group=${hashByGroupAndName('workspace', 'workspace1')}`,

      status: 200,
      response: [],
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RETRY').click();
      });

    cy.findByTestId('snackbar').should(
      'have.text',
      'Successfully Restart workspace workspace1.',
    );

    cy.findByText('100%');

    cy.findByText('CLOSE').parent('button').should('be.enabled').click();
  });

  it('retry start worker should be used normally in restart workspace', () => {
    cy.switchSettingSection(
      SETTING_SECTION.dangerZone,
      'Restart this workspace',
    );

    cy.route({
      method: 'PUT',
      url: 'api/workers/*/start**',
      status: 403,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RESTART').click();
      });

    cy.findByText('ERROR', { exact: false });

    cy.request('PUT', 'api/workers/workspace1/start?group=worker');

    cy.route({
      method: 'PUT',
      url: 'api/workers/*/start**',
      status: 202,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RETRY').click();
      });

    cy.findByTestId('snackbar').should(
      'have.text',
      'Successfully Restart workspace workspace1.',
    );

    cy.findByText('100%');

    cy.findByText('CLOSE').parent('button').should('be.enabled').click();
  });

  it('the rollback workspace should return to its original state', () => {
    cy.switchSettingSection(
      SETTING_SECTION.dangerZone,
      'Restart this workspace',
    );

    cy.route({
      method: 'PUT',
      url: 'api/workers/*/start**',
      status: 403,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RESTART').click();
      });

    cy.findByText('ERROR', { exact: false });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('ROLLBACK').click();
      });

    cy.route({
      method: 'PUT',
      url: 'api/workers/*/start**',
      status: 203,
      response: {},
    });

    cy.findByText('15%');

    cy.request('PUT', 'api/workers/workspace1/start?group=worker');

    cy.findByText('0%');

    cy.findByText('CLOSE').parent('button').should('be.enabled').click();
  });

  it('should mark the workspace as unstable when it fails', () => {
    cy.switchSettingSection(
      SETTING_SECTION.dangerZone,
      'Restart this workspace',
    );

    cy.route({
      method: 'PUT',
      url: 'api/workers/workspace1?group=worker',
      status: 403,
      response: {},
    });

    cy.findAllByRole('dialog')
      .filter(':visible')
      .should('have.length', 1)
      .within(() => {
        cy.findByText('RESTART').click();
      });

    cy.findByText('ERROR', { exact: false }).should('have.length', 1);

    cy.findByText('CLOSE').parent('button').should('be.enabled').click();

    cy.findByText('ABORT').parent('button').click();

    // close the snackbar
    cy.findByTestId('snackbar').find('button:visible').click();

    // close the settings dialog
    cy.findByTestId('workspace-settings-dialog-close-button')
      .should('be.visible')
      .click();

    cy.get('span[title="Unstable workspace"]').should('have.length', 1);
  });
});
