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
import { LOG_SERVICE } from '../../../src/api/apiInterface/logInterface';
import { SETTING_SECTION } from '../../support/customCommands';
import { KIND, DevToolTabName } from '../../../src/const';

describe('App Bar', () => {
  // generate variables
  const t1 = generate.serviceName({ prefix: 'topic' });
  let hostname = '';

  before(() => {
    cy.deleteAllServices();
    cy.createWorkspace({});
  });

  beforeEach(() => {
    // our tests should begin from home page
    cy.visit(`/`, {
      onBeforeLoad(win) {
        // to surveillance the window.open() event
        // we stub it and do nothing
        cy.stub(win, 'open');
      },
    });
  });

  context('DevTool', () => {
    it.only('should see topic list and data in devTool after creating a topic', () => {
      cy.createPipeline();

      // add shared topics
      cy.switchSettingSection(SETTING_SECTION.topics);
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
      cy.findByText(t1)
        .parent('tr')
        // the new added topic should exist and running
        .contains('td', 'RUNNING');
      // press back button to home page
      cy.findByTestId('workspace-settings-dialog-close-button').click();

      // drag the new add topic into paper
      cy.addElement({ name: t1, kind: KIND.topic });

      // topic list of devTool should not be empty
      cy.findByTitle(/developer tools/i).click();
      cy.findByTestId('topic-list-select').click();

      cy.get('ul[role="listbox"]').findByText(t1).click();

      cy.findByTestId('view-topic-table')
        .find('tbody tr')
        .should('have.length.greaterThan', 0)
        .within(() => {
          // assert each cell data
          cy.findByText('b');
          cy.findByText('123');
          cy.findByText('["c","d","e"]');
          cy.findByText('{"a":"aaa"}');
          cy.findByText('true');

          cy.get('td')
            // get the view topic button
            .first()
            .find('svg')
            .click();
        });

      cy.findAllByRole('dialog')
        .filter(':visible')
        .should('have.length', 1)
        .within(() => {
          // assert title
          cy.findByText('View topic source').should('exist');
          // assert content
          cy.get('.react-json-view').should('exist');

          cy.findByTestId('close-button').click();
        });
    });

    it('should be able to change the topic settings', () => {
      cy.findByTitle(/developer tools/i).click();

      // switch to topics tab
      cy.findByRole('tablist').filter(':visible').find('button').eq(0).click();

      // we could not click the buttons in header if the log type is empty
      cy.findByTitle('Fetch the data again')
        .find('button')
        .should('be.disabled');
      cy.findByTitle('Query with different parameters')
        .find('button')
        .should('be.disabled');
      cy.findByTitle('Open in a new window')
        .find('button')
        .should('be.disabled');
      cy.get('.status-bar').findByText('No topic data').should('exist');

      cy.findByTestId('topic-list-select').click();
      cy.get('ul[role="listbox"]:visible li').findByText(t1).click();

      // toggle the devTool
      cy.findByTitle('Close this panel').click();
      cy.findByTitle(/developer tools/i).click();

      // the previous state should be remembered
      cy.findByTestId('topic-list-select')
        .findByRole('button')
        .invoke('html')
        .should('equal', t1);

      // the topic data should not be empty
      cy.findByTestId('view-topic-table')
        .find('tbody tr')
        .should('have.length.greaterThan', 0);

      // refetch topic
      cy.findByTitle('Fetch the data again').click();
      cy.findByTestId('view-topic-table')
        .find('tbody tr')
        .should('have.length.greaterThan', 0);

      // query settings
      cy.findByTitle('Query with different parameters').click();
      cy.findByTestId('topic-query-popover').findByText('QUERY').click();
      cy.findByTestId('view-topic-table').should('exist');

      cy.findByTestId('topic-query-popover').within(() => {
        cy.get('input[type=number]').first().clear().type('200');
      });
      cy.findByTestId('topic-query-popover').findByText('QUERY').click();

      // the topic only has 1 record
      // we will get 1 row data in statusBar even if we set the settings to 200
      cy.get('.status-bar').findByText('1 rows per query').should('exist');

      // re-open the settings
      cy.findByTestId('topic-query-popover').trigger('keydown', {
        keyCode: 27,
        which: 27,
      });
      cy.findByTitle('Query with different parameters').click();
      cy.findByTestId('topic-query-popover').trigger('keydown', {
        keyCode: 27,
        which: 27,
      });
      cy.findByTestId('view-topic-table').should('exist');

      // open new window button
      cy.findByTitle('Open in a new window').click();
      cy.window().its('open').should('be.called');
    });

    it('open new window for topic data', () => {
      cy.visit(
        `/view?type=${DevToolTabName.TOPIC}&topicName=${t1}&topicLimit=10`,
      );
      cy.findByTestId('data-window').children().should('be.visible');
    });

    it('should be able to switch to log tab', () => {
      cy.findByTitle(/developer tools/i).click();

      // switch to logs tab
      cy.findByRole('tablist').filter(':visible').find('button').eq(1).click();
      cy.findByTestId('log-type-select').click();
      cy.get('ul[role="listbox"]:visible')
        .should('have.length', 1)
        .find('li')
        .should('have.length', Object.keys(LOG_SERVICE).length)
        .each(($el) => {
          expect(Object.keys(LOG_SERVICE)).contains($el.text());
        })
        // select the last element of list to close this dropdown list
        .last()
        .click();

      cy.findByTestId('log-type-select').click();
      cy.get('ul[role="listbox"]:visible li')
        .findByText(LOG_SERVICE.configurator)
        .click();

      cy.findByTestId('log-hostname-select')
        .findByRole('button')
        .invoke('html')
        .should('not.be.empty');

      cy.get('.status-bar').findByText('Latest 10 minutes').should('exist');
    });

    it('should be able to change the log settings', () => {
      cy.findByTitle(/developer tools/i).click();

      // switch to logs tab
      cy.findByRole('tablist').filter(':visible').find('button').eq(1).click();

      // we could not click the buttons in header if the log type is empty
      cy.findByTitle('Fetch the data again')
        .find('button')
        .should('be.disabled');
      cy.findByTitle('Query with different parameters')
        .find('button')
        .should('be.disabled');
      cy.findByTitle('Open in a new window')
        .find('button')
        .should('be.disabled');
      cy.get('.status-bar').findByText('No log data').should('exist');

      cy.findByTestId('log-type-select').click();
      cy.get('ul[role="listbox"]:visible li')
        .findByText(LOG_SERVICE.configurator)
        .click();

      // toggle the devTool
      cy.findByTitle('Close this panel').click();
      cy.findByTitle(/developer tools/i).click();

      // the previous state should be remembered
      cy.findByTestId('log-hostname-select')
        .findByRole('button')
        .invoke('html')
        .then((name) => (hostname = name))
        .should('not.be.empty');

      // the log data should not be empty
      cy.findByTestId('view-log-list')
        .findByRole('rowgroup')
        .children('div')
        .should('have.length.greaterThan', 0);

      // refetch log
      cy.findByTitle('Fetch the data again').click();
      cy.findByTestId('view-log-list')
        .findByRole('rowgroup')
        .children('div')
        .should('have.length.greaterThan', 0);

      // query settings
      cy.findByTitle('Query with different parameters').click();
      cy.findByTestId('log-query-popover').findByText('QUERY').click();
      cy.findByTestId('view-log-list').should('exist');

      cy.findByTestId('log-query-popover').within(() => {
        cy.get('input[type=number]').first().clear().type('1000');
      });
      cy.findByTestId('log-query-popover').findByText('QUERY').click();

      cy.get('.status-bar').as('statusBar');
      cy.get('@statusBar').findByText('Latest 1000 minutes').should('exist');

      // re-open the settings
      cy.findByTestId('log-query-popover').trigger('keydown', {
        keyCode: 27,
        which: 27,
      });
      cy.findByTitle('Query with different parameters').click();

      cy.findByTestId('log-query-popover').within(() => {
        cy.findAllByRole('radio')
          // customize
          .last()
          .click();

        cy.findByLabelText('Start date').then(($statEl) => {
          cy.get('@statusBar')
            .find('div')
            .invoke('html')
            .should(
              'include',
              `Customize from ${$statEl
                .val()
                ?.toString()
                .split('T')[0]
                .replace(/-/g, '/')}`,
            )
            .should('exist');
        });
      });
      cy.findByTestId('log-query-popover').trigger('keydown', {
        keyCode: 27,
        which: 27,
      });

      // open new window button
      cy.findByTitle('Open in a new window').click();
      cy.window().its('open').should('be.called');
    });

    it('open new window for log data', () => {
      cy.visit(
        `/view?type=${DevToolTabName.LOG}&logType=configurator`
          .concat(`&hostname=${hostname}`)
          .concat(`&timeGroup=latest&timeRange=10`),
      );
      cy.findByTestId('data-window').children().should('be.visible');
    });

    it('should not able to select cell in dropdown list if type is shabondi or stream', () => {
      cy.findByTitle(/developer tools/i).click();

      // switch to logs tab
      cy.findByRole('tablist').filter(':visible').find('button').eq(1).click();

      // select shabondi type
      cy.findByTestId('log-type-select').click();
      cy.get('ul[role="listbox"]:visible')
        .contains('li', 'shabondi')
        .first()
        .click();
      // there is no cells exists
      cy.findByTestId('log-cell-select').click();
      cy.get('ul[role="listbox"]').should('not.exist');

      // select stream type
      cy.findByTestId('log-type-select').click();
      cy.get('ul[role="listbox"]:visible li').contains('li', 'stream').click();
      // there is no cells exists
      cy.findByTestId('log-cell-select').click();
      cy.get('ul[role="listbox"]').should('not.exist');
    });
  });
});
