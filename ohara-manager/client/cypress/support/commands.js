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

import 'cypress-testing-library/add-commands';

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

Cypress.Commands.add('deleteAllNodes', () => {
  Cypress.log({
    name: 'DELETE_ALL_NODES',
  });

  const _ = Cypress._;

  cy.request('GET', 'api/nodes')
    .then(res => res.body)
    .then(nodes => {
      if (!_.isEmpty(nodes)) {
        _.forEach(nodes, node => {
          cy.request('DELETE', `api/nodes/${node.name}`);
        });
      }
    });
});

Cypress.Commands.add('insertNode', node => {
  Cypress.log({
    name: 'INSERT_NODE',
  });

  cy.request('POST', 'api/nodes', {
    name: node.name,
    port: node.port,
    user: node.user,
    password: node.password,
  });
});

Cypress.Commands.add('testNodeCheck', () => {
  Cypress.log({
    name: 'TEST_NODE_CHECK',
  });
  const _ = Cypress._;

  cy.request('GET', 'api/nodes')
    .then(res => res.body)
    .then(nodes => {
      if (_.isEmpty(nodes)) {
        Cypress.log({
          name: 'ADD_TEST_NODE',
        });
        cy.request('POST', 'api/nodes', {
          name: Cypress.env('nodeName'),
          port: Cypress.env('nodePort'),
          user: Cypress.env('nodeID'),
          password: Cypress.env('nodePW'),
        });
      }
    });
});

Cypress.Commands.add('deleteAllPipelines', () => {
  Cypress.log({
    name: 'DELETE_ALL_PIPELINES',
  });

  const _ = Cypress._;

  cy.request('GET', 'api/pipelines')
    .then(res => res.body)
    .then(pipelines => {
      if (!_.isEmpty(pipelines)) {
        _.forEach(pipelines, pipeline => {
          cy.request('DELETE', `api/pipelines/${pipeline.id}`);
        });
      }
    });
});

Cypress.Commands.add('insertPipeline', (cluster, pipeline) => {
  Cypress.log({
    name: 'INSERT_PIPELINE',
  });

  cy.request('POST', `/api/pipelines`, {
    name: pipeline.name || 'Untitled pipeline',
    rules: pipeline.rules || {},
    workerClusterName: cluster,
  });
});

/**
 * Usage: cy.get('input[type=file]').uploadFile('example.json')
 */
Cypress.Commands.add(
  'uploadFile',
  { prevSubject: 'element' },
  (subject, fileName) => {
    Cypress.log({
      name: 'UPLOAD_FILE',
    });

    return cy.fixture(fileName, 'binary').then(content => {
      const el = subject[0];
      const testFile = new File([content], fileName);
      const dataTransfer = new DataTransfer();
      dataTransfer.items.add(testFile);
      el.files = dataTransfer.files;
    });
  },
);
Cypress.Commands.add('uploadPlugin', (selector, fixturePath, name, type) => {
  Cypress.log({
    name: 'UPLOAD_PLUGIN',
  });

  cy.get(selector).then(subject =>
    cy.window().then(win =>
      cy
        .fixture(fixturePath, 'base64')
        .then(Cypress.Blob.base64StringToBlob)
        .then(blob => {
          const el = subject[0];
          const testFile = new win.File([blob], name, { type });
          const dataTransfer = new win.DataTransfer();
          dataTransfer.items.add(testFile);
          el.files = dataTransfer.files;
          cy.wrap(subject).trigger('change', { force: true });
        }),
    ),
  );
});

Cypress.Commands.add('deleteAllTopics', () => {
  Cypress.log({
    name: 'DELETE_ALL_TOPICS',
  });

  const _ = Cypress._;

  cy.request('GET', 'api/topics')
    .then(res => res.body)
    .then(topics => {
      if (!_.isEmpty(topics)) {
        _.forEach(topics, topic => {
          cy.request('DELETE', `api/topics/${topic.id}`);
        });
      }
    });
});
