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
import { hashByGroupAndName } from '../../../src/utils/sha';
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
  // generate fake hostname
  const hostname = generate.serviceName();

  // we use the default workspace name and group for file tags
  const workspaceKey = {
    name: 'workspace1',
    group: 'workspace',
  };
  const fileGroup = hashByGroupAndName(workspaceKey.group, workspaceKey.name);

  // create files
  const source = {
    fixturePath: 'jars',
    name: 'ohara-it-source.jar',
    group: fileGroup,
  };
  const sink = {
    fixturePath: 'jars',
    name: 'ohara-it-sink.jar',
    group: fileGroup,
  };
  const stream = {
    fixturePath: 'jars',
    name: 'ohara-it-stream.jar',
    group: fileGroup,
  };
  const fakeJar = {
    name: 'fake.jar',
    group: fileGroup,
  };
  const files = [source, sink, stream];

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
          cy.contains('button', 'CREATE').click();
        });

      // the new added topic should exist and running
      cy.findByText(t1).parent('tr').contains('td', 'RUNNING');

      cy.findByTitle('View topic').click();
      cy.findAllByRole('dialog')
        .filter(':visible')
        .within(() => {
          cy.get('table')
            // we should only have 1 table (information) in fake mode
            .first()
            .within(($table) => {
              // assert topic name
              cy.getTableCellByColumn($table, 'Name', t1).should('exist');
              // this topic should be shared
              cy.getTableCellByColumn($table, 'Type', 'Shared').should('exist');
            });
        })
        // press "ESC" again back to topic list
        .trigger('keydown', { keyCode: 27, which: 27 });

      // delete topic
      cy.findByTitle('Delete topic').click();
      // cancel deletion
      cy.findAllByRole('dialog')
        .filter(':visible')
        .within(() => {
          cy.contains('button', 'CANCEL').click();
        });
      cy.findByTitle('Delete topic').click();
      cy.findAllByRole('dialog')
        .filter(':visible')
        .within(() => {
          cy.contains('button', 'DELETE').click();
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
          cy.contains('button', 'CREATE').click();
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
          cy.contains('button', 'CREATE').click();
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
          cy.contains('button', 'DELETE').click();
        });

      // the t2 topic should not exist
      cy.findByText(t2).should('not.exist');

      cy.findByTitle('Delete topic').click();
      cy.findAllByRole('dialog')
        .filter(':visible')
        .within(() => {
          cy.contains('button', 'DELETE').click();
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
          cy.contains('button', 'CANCEL').click();
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

      cy.contains('button', 'ADD AUTOFILL').click();

      // add a setting filling
      cy.get('input[name="displayName"]').type(setting);

      cy.contains('button', 'ADD KEY').click();
      cy.get('input[name="settings[0].key"]').type('completed');
      cy.findByText('completed.folder').click();
      cy.get('input[name="settings[0].value"]').type('fake');

      cy.contains('button', 'ADD KEY').click();
      cy.get('input[name="settings[1].key"]').type('ftp.p');
      cy.findByText('ftp.port').click();
      cy.get('input[name="settings[1].value"]').type('22');

      cy.contains('button', 'ADD KEY').click();
      cy.get('input[name="settings[2].key"]').type('error');
      cy.findByText('error.folder').click();

      // remove the last one key-value pair
      cy.findAllByTitle('Delete the set of key, value')
        .filter(':visible')
        .last()
        .click();

      // click save button
      cy.contains('button', 'SAVE').click();
      cy.findByText(setting).should('exist');

      // edit this setting
      cy.findAllByTitle('Edit the autofill').filter(':visible').click();
      // could change name and add new key
      cy.get('input[name="displayName"]').clear().type('foo');
      cy.contains('button', 'ADD KEY').click();
      cy.get('input[name="settings[2].key"]').type('ftp.ho');
      cy.findByText('ftp.hostname').click();

      // click save button
      cy.contains('button', 'SAVE').click();
      // the origin setting name should not exist
      cy.findByText(setting).should('not.exist');

      // copy this setting
      cy.findAllByTitle('Copy the autofill').filter(':visible').click();
      cy.get('input[name="displayName"]').clear().type(setting);
      cy.contains('button', 'SAVE').click();

      // we have two settings now
      cy.get('div.section-page-content').within(() => {
        cy.get('ul li').should('have.length', 2);
      });

      // remove all settings
      cy.findAllByTitle('Delete the autofill')
        .filter(':visible')
        .each(($el) => {
          cy.wrap($el).click();
          cy.contains('button', 'DELETE').filter(':visible').click();
        });
    });

    it('could not add duplicate name or key of setting fillings', () => {
      cy.switchSettingSection(SETTING_SECTIONS.autofill);

      // add a setting filling
      cy.contains('button', 'ADD AUTOFILL').click();
      cy.get('input[name="displayName"]').type(setting);

      cy.contains('button', 'ADD KEY').click();
      cy.get('input[name="settings[0].key"]').type('completed');
      cy.findByText('completed.folder').click();
      cy.get('input[name="settings[0].value"]').type('fake');

      cy.contains('button', 'ADD KEY').click();
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
      cy.contains('button', 'SAVE').click();
      cy.findByText(setting).should('exist');

      // add a same name setting filling
      cy.contains('button', 'ADD AUTOFILL').click();
      cy.get('input[name="displayName"]').type(setting).blur();
      cy.findByText(
        `Name '${setting}' already existed. Please use a different name`,
      ).should('exist');

      // click cancel button
      cy.contains('button', 'CANCEL').click();

      // remove setting
      cy.findAllByTitle('Delete the autofill')
        .filter(':visible')
        .each(($el) => {
          cy.wrap($el).click();
          // click cancel deletion
          cy.contains('button', 'CANCEL').filter(':visible').click();
          // remove setting again
          cy.wrap($el).click();
          cy.contains('button', 'DELETE').filter(':visible').click();
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
          cy.get('table')
            .first()
            .within(($table) => {
              // assert each field
              cy.getTableCellByColumn($table, 'Hostname', node.hostname).should(
                'exist',
              );
              cy.getTableCellByColumn(
                $table,
                'Port',
                node.port.toString(),
              ).should('exist');
              cy.getTableCellByColumn($table, 'User', node.user).should(
                'exist',
              );
              cy.getTableCellByColumn($table, 'Password', node.password).should(
                'exist',
              );
            });

          cy.get('table')
            .last<HTMLTableElement>()
            .within(($table) => {
              // assert zookeeper should exist in services list
              // the name of zookeeper should be as same as "workspace"
              cy.getTableCellByColumn($table, 'Name', 'workspace1').should(
                'exist',
              );

              // assert zookeeper should exist in services type
              cy.getTableCellByColumn($table, 'Type', 'zookeeper').should(
                'exist',
              );
            });
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
          cy.get('table')
            .first()
            .within(($table) => {
              // assert each field
              cy.getTableCellByColumn($table, 'Hostname', node.hostname).should(
                'exist',
              );
              cy.getTableCellByColumn(
                $table,
                'Port',
                node.port.toString(),
              ).should('exist');
              cy.getTableCellByColumn($table, 'User', node.user).should(
                'exist',
              );
              cy.getTableCellByColumn($table, 'Password', node.password).should(
                'exist',
              );
            });

          cy.get('table')
            .last<HTMLTableElement>()
            .within(($table) => {
              // assert broker should exist in services list
              // the name of broker should be as same as "workspace"
              cy.getTableCellByColumn($table, 'Name', 'workspace1').should(
                'exist',
              );

              // assert broker should exist in services type
              cy.getTableCellByColumn($table, 'Type', 'broker').should('exist');
            });
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
          cy.get('table')
            .first()
            .within(($table) => {
              // assert each field
              cy.getTableCellByColumn($table, 'Hostname', node.hostname).should(
                'exist',
              );
              cy.getTableCellByColumn(
                $table,
                'Port',
                node.port.toString(),
              ).should('exist');
              cy.getTableCellByColumn($table, 'User', node.user).should(
                'exist',
              );
              cy.getTableCellByColumn($table, 'Password', node.password).should(
                'exist',
              );
            });

          cy.get('table')
            .last<HTMLTableElement>()
            .within(($table) => {
              // assert worker should exist in services list
              // the name of worker should be as same as "workspace"
              cy.getTableCellByColumn($table, 'Name', 'workspace1').should(
                'exist',
              );

              // assert worker should exist in services type
              cy.getTableCellByColumn($table, 'Type', 'worker').should('exist');
            });
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

  context('Files Settings', () => {
    it('should able to add and remove files', () => {
      cy.switchSettingSection(SETTING_SECTIONS.files);

      cy.get('div.section-page-content').within(() => {
        // upload the files by custom command "createJar"
        files.forEach((file) => {
          cy.get('input[type="file"]').then((element) => {
            cy.createJar(file).then((params) => {
              (element[0] as HTMLInputElement).files = params.fileList;
              cy.wrap(element).trigger('change', { force: true });
            });
          });
        });

        // after upload file, click the upload file again
        cy.wait(1000);
        cy.findByTitle('Upload File').click();

        cy.findByText(source.name).should('exist');
        cy.findByText(sink.name).should('exist');
        cy.findByText(stream.name).should('exist');

        // check the source file could be removed
        cy.get('table')
          .should('have.length', 1)
          .within(($table) => {
            cy.getTableCellByColumn($table, 'Name', source.name)
              .siblings('td')
              .last()
              .within((el$) => {
                el$.find('div[title="Delete file"]').click();
              });
          });
      });
      // confirm dialog
      cy.findByTestId('confirm-button-DELETE').click();

      // after removed, the file should not be existed
      cy.get('div.section-page-content').within(() => {
        cy.get('table')
          .should('have.length', 1)
          .within(($table) => {
            cy.getTableCellByColumn($table, 'Name', source.name).should(
              'not.exist',
            );
          });

        //filter
        cy.findAllByPlaceholderText('Search')
          .filter(':visible')
          .type(stream.name);
        cy.findByText(stream.name).should('exist');
        cy.findByText(sink.name).should('not.exist');

        // view the classNames of stream file
        cy.get('table')
          .should('have.length', 1)
          .within(($table) => {
            cy.getTableCellByColumn($table, 'Name', stream.name)
              .siblings('td')
              .last()
              .within((el$) => {
                el$.find('div[title="View file"]').click();
              });
          });
      });
      // assert the class detail of stream
      cy.findAllByText('DumbStream', { exact: false }).should('exist');
    });

    it('should able to select file in stream jars', () => {
      cy.switchSettingSection(SETTING_SECTIONS.stream);
      cy.get('div.section-page-content').within(() => {
        cy.findByTitle('Add File').click();
      });

      // select the stream jar we just added
      cy.contains('h3', 'Select file').filter(':visible').should('exist');
      cy.findByText(stream.name).should('exist');
      cy.findAllByRole('dialog')
        .filter(':visible')
        .should('have.length', 1)
        .within(() => {
          cy.get('table')
            .should('have.length', 1)
            .within(($table) => {
              cy.getTableCellByColumn($table, 'Name', stream.name)
                .siblings('td')
                .first()
                .find('[type="checkbox"]')
                .check();
            });
          cy.findByText('SAVE').click();
        });

      // the selected jar should appear
      cy.get('div.section-page-content').within(() => {
        cy.findByText(stream.name).should('exist');
        cy.get('table')
          .should('have.length', 1)
          .within(($table) => {
            cy.getTableCellByColumn($table, 'Name', stream.name).should(
              'exist',
            );
            cy.getTableCellByColumn($table, 'Valid')
              .should('exist')
              // the upload jar should be valid
              .find('svg[severity="success"]')
              .should('exist');
          });
      });

      // upload a fake jar
      cy.get('div.section-page-content').within(() => {
        cy.findByTitle('Add File').click();
      });

      cy.findAllByRole('dialog')
        .filter(':visible')
        .should('have.length', 1)
        .within(() => {
          cy.get('input[type="file"]').then((element) => {
            const file = new File([], fakeJar.name);
            const dataTransfer = new DataTransfer();
            dataTransfer.items.add(file);
            const fileList = dataTransfer.files;
            (element[0] as HTMLInputElement).files = fileList;
            cy.wrap(element).trigger('change', { force: true });
          });
        });
      // after upload file, click the upload file again
      cy.wait(1000);
      cy.findByTitle('Upload File').click();

      // select the fake jar and de-select stream jar
      cy.findAllByRole('dialog')
        .filter(':visible')
        .should('have.length', 1)
        .within(() => {
          cy.get('table')
            .should('have.length', 1)
            .within(($table) => {
              cy.getTableCellByColumn($table, 'Name', stream.name)
                .siblings('td')
                .first()
                .find('[type="checkbox"]')
                .uncheck();
              cy.getTableCellByColumn($table, 'Name', fakeJar.name)
                .siblings('td')
                .first()
                .find('[type="checkbox"]')
                .check();
            });
          cy.findByText('SAVE').click();
        });

      // the selected jar should appear
      cy.get('div.section-page-content').within(() => {
        cy.findByText(fakeJar.name).should('exist');
        cy.get('table')
          .should('have.length', 1)
          .within(($table) => {
            cy.getTableCellByColumn($table, 'Name', fakeJar.name).should(
              'exist',
            );
            cy.getTableCellByColumn($table, 'Valid')
              .should('exist')
              // the upload fake jar should not be valid
              .find('svg[severity="warning"]')
              .should('exist');
          });
      });

      // after select the fake jar as stream
      // we could not remove it from file list
      cy.get('div.section-page-content').within(() => {
        cy.findByTitle('Add File').click();
      });
      cy.findAllByRole('dialog')
        .filter(':visible')
        .should('have.length', 1)
        .within(() => {
          cy.get('table')
            .should('have.length', 1)
            .within(($table) => {
              cy.getTableCellByColumn(
                $table,
                'Used',
                undefined,
                (row) => row.has(`td:contains("${fakeJar.name}")`).length > 0,
              )
                .invoke('html')
                .should('equal', '<div>Stream</div>');

              // used jar could not be removed
              cy.getTableCellByColumn(
                $table,
                'Actions',
                undefined,
                (row) => row.has(`td:contains("${fakeJar.name}")`).length > 0,
              ).within(() => {
                cy.findByTitle('Cannot delete files that are in use').should(
                  'exist',
                );
              });

              // delete stream file
              cy.getTableCellByColumn(
                $table,
                'Actions',
                undefined,
                (row) => row.has(`td:contains("${stream.name}")`).length > 0,
              ).within(() => {
                cy.findByTitle('Delete file').click();
              });
            });
        });

      cy.findByTestId('confirm-button-DELETE').click();
      // Since the file list table will be altered by redux state
      // we need to wait for a period for changes applied
      cy.wait(500);
      cy.findByText(stream.name).should('not.exist');

      // close file list and de-select fake jar as stream
      cy.findAllByRole('dialog')
        .filter(':visible')
        .should('have.length', 1)
        .within(() => {
          cy.findByText('CANCEL').click();
        });
      cy.findByTitle('Remove file').click();
      cy.findAllByText('REMOVE').filter(':visible').click();
      cy.findByText(fakeJar.name).should('not.exist');

      // after de-select the fake jar
      // we could remove it from file list
      cy.get('div.section-page-content').within(() => {
        cy.findByTitle('Add File').click();
      });
      cy.findAllByRole('dialog')
        .filter(':visible')
        .should('have.length', 1)
        .within(() => {
          cy.get('table')
            .should('have.length', 1)
            .within(($table) => {
              cy.getTableCellByColumn(
                $table,
                'Used',
                undefined,
                (row) => row.has(`td:contains("${fakeJar.name}")`).length > 0,
              )
                .invoke('html')
                .should('be.empty');

              // delete fake file
              cy.getTableCellByColumn(
                $table,
                'Actions',
                undefined,
                (row) => row.has(`td:contains("${fakeJar.name}")`).length > 0,
              ).within(() => {
                cy.findByTitle('Delete file').click();
              });
            });
        });

      cy.findByTestId('confirm-button-DELETE').click();
      // Since the file list table will be altered by redux state
      // we need to wait for a period for changes applied
      cy.wait(500);
      cy.findByText(fakeJar.name).should('not.exist');
    });

    it('should able to select file in worker plugins', () => {
      cy.switchSettingSection(
        SETTING_SECTIONS.worker,
        'Worker plugins and shared jars',
      );

      cy.get('div.plugins').findByTitle('Add File').click();

      // select the sink jar
      cy.findByText(sink.name).should('exist');
      cy.findAllByRole('dialog')
        .filter(':visible')
        .should('have.length', 1)
        .within(() => {
          cy.get('table')
            .should('have.length', 1)
            .within(($table) => {
              cy.getTableCellByColumn($table, 'Name', sink.name)
                .siblings('td')
                .first()
                .find('[type="checkbox"]')
                .check();
            });
          cy.findByText('SAVE').click();
        });
      cy.findByText(sink.name).should('exist');

      // add the same file to shared jars
      cy.get('div.shared-jars').findByTitle('Add File').click();
      cy.findAllByRole('dialog')
        .filter(':visible')
        .should('have.length', 1)
        .within(() => {
          cy.get('table')
            .should('have.length', 1)
            .within(($table) => {
              cy.getTableCellByColumn($table, 'Name', sink.name)
                .siblings('td')
                .first()
                .find('[type="checkbox"]')
                .check();
            });
          cy.findByText('SAVE').click();
        });

      // we have uploaded two jars into worker
      cy.findAllByText(sink.name).should('have.length', 2);

      // undo uploaded jar into shared jars
      cy.get('div.shared-jars').findByTitle('Undo add file').click();
      cy.findAllByText(sink.name).should('have.length', 1);

      cy.get('div.section-page-header').within(() => {
        // back to Settings dialog
        cy.get('button').click();
      });

      // the worker section should have 1 change warning
      cy.contains('h2', SETTING_SECTIONS.worker)
        .parent('section')
        .find('ul')
        .contains('span', '1');

      // restart worker
      cy.switchSettingSection(
        SETTING_SECTIONS.dangerZone,
        'Restart this worker',
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
          // expand the log process
          cy.findByText('Stop worker', { exact: false });
          cy.findByText('Start worker', { exact: false });

          cy.get('div.FlexFooterDiv')
            .contains('button', 'CLOSE')
            .should('be.enabled')
            .click();
        });
      // close the snackbar
      cy.findByTestId('snackbar').find('button:visible').click();

      // close the settings dialog
      cy.findByTestId('workspace-settings-dialog-close-button')
        .should('be.visible')
        .click();
    });
  });

  context('Danger Zone', () => {
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
      cy.findByTestId('fullscreen-dialog-close-button').click();

      // add new added node into workspace
      cy.switchSettingSection(SETTING_SECTIONS.nodes);
      cy.get('div.section-page-content').within(() => {
        cy.findByTitle('Add Node').click();
      });

      // re-open select node
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
              // checked the new added hostname
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
          // there is no service assign to this node yet
          .should('be.empty');
      });
    });

    it('should show an restart indicator after adding node to zookeeper', () => {
      cy.switchSettingSection(SETTING_SECTIONS.zookeeper);

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
              // checked the new added hostname
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
              // checked the new added hostname
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
      cy.contains('h2', SETTING_SECTIONS.zookeeper)
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
      cy.contains('h2', SETTING_SECTIONS.zookeeper)
        .parent('section')
        .find('ul')
        .contains('span', '1')
        .should('not.exist');
    });

    it('should able to restart from indicator after adding node to zookeeper', () => {
      // add node to zookeeper
      cy.switchSettingSection(SETTING_SECTIONS.zookeeper);
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
              // checked the new added hostname
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
          cy.findByText('Stop broker', { exact: false });
          cy.findByText('Stop zookeeper', { exact: false });
          cy.findByText('Start zookeeper', { exact: false });
          cy.findByText('Start broker', { exact: false });
          cy.findByText('Start worker', { exact: false });

          cy.get('div.FlexFooterDiv')
            .contains('button', 'CLOSE')
            .should('be.enabled')
            .click();
        });

      // close the snackbar
      cy.findByTestId('snackbar').find('button:visible').click();

      // close the settings dialog
      cy.findByTestId('workspace-settings-dialog-close-button')
        .should('be.visible')
        .click();

      cy.switchSettingSection(SETTING_SECTIONS.zookeeper);
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
      cy.switchSettingSection(
        SETTING_SECTIONS.dangerZone,
        'Restart this worker',
      );
      cy.findAllByRole('dialog')
        .filter(':visible')
        .should('have.length', 1)
        .within(() => {
          cy.findByText('CANCEL').click();
        });

      // re-click again
      cy.switchSettingSection(
        SETTING_SECTIONS.dangerZone,
        'Restart this worker',
      );

      cy.findAllByRole('dialog')
        .filter(':visible')
        .should('have.length', 1)
        .within(() => {
          cy.findByText('RESTART').click();
        });
    });
  });
});
