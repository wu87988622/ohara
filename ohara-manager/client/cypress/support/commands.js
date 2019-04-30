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

import { makeRandomPort, getFakeNode, makeRandomStr } from '../utils';
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

Cypress.Commands.add('registerWorker', workerName => {
  const fileName = '../scripts/servicesApi/service.json';
  const update = { name: workerName, serviceType: 'workers' };

  cy.task('readFileMaybe', fileName).then(data => {
    if (!data) {
      // Create a new file if there's no one
      cy.writeFile(fileName, [update]);
      return;
    }

    // Append a new worker to the existing file
    cy.writeFile(fileName, [...data, update]);
  });
});

Cypress.Commands.add('createWorker', () => {
  cy.log('Create a new worker');

  const { name: nodeName } = getFakeNode();
  const workerName = 'wk' + makeRandomStr();
  Cypress.env('WORKER_NAME', workerName);

  cy.registerWorker(workerName);
  cy.request('GET', 'api/brokers')
    .then(res => res.body[0]) // there should only be one broker in the list
    .as('broker');

  cy.get('@broker').then(broker => {
    cy.request('POST', 'api/workers', {
      name: workerName,
      jars: [],
      brokerClusterName: broker.name,
      nodeNames: [nodeName],
      clientPort: makeRandomPort(),
      jmxPort: makeRandomPort(),
    });
  });

  // Make a request to configurator see if worker cluster is ready for use
  const req = endPoint => {
    cy.request('GET', endPoint).then(res => {
      // When connectors field has the right connector info
      // this means everything is ready to be tested
      if (res.body.connectors.length > 0) return;

      // if it's not ready yet, wait a bit and make another request
      cy.wait(1500);
      req(endPoint);
    });
  };

  const endPoint = `api/workers/${workerName}`;
  cy.request('GET', endPoint).then(() => req(endPoint));
});

Cypress.Commands.add('deleteWorker', () => {
  cy.log('Delete previous created worker if there is one ');

  const req = (endPoint, serviceName) => {
    // We need to wait for specific service removed from the node
    // then we can move on and remove another service
    cy.request('GET', endPoint).then(res => {
      const isServiceExit = res.body.some(
        service => service.name === serviceName,
      );

      // Target service is not in the list anymore, break the loop
      if (!isServiceExit) return;

      // Wait and make another request
      cy.wait(1500);
      req(endPoint, serviceName);
    });
  };

  const workerName = Cypress.env('WORKER_NAME');
  cy.request('GET', 'api/workers').then(res => {
    const hasWorker = res.body.some(worker => worker.name === workerName);

    if (hasWorker) {
      cy.request('DELETE', `api/workers/${workerName}?force=true`).then(() =>
        req('api/workers', workerName),
      );
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

Cypress.Commands.add('deleteAllPipelines', () => {
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

Cypress.Commands.add('deleteTopic', topicName => {
  cy.request('GET', 'api/topics').then(res => {
    res.body.forEach(({ name, id }) => {
      if (name === topicName) {
        cy.request('DELETE', `api/topics/${id}`);
      }
    });
  });
});

Cypress.Commands.add('deletePipeline', pipelineName => {
  cy.request('GET', 'api/pipelines').then(res => {
    res.body.forEach(pipeline => {
      if (pipeline.name === pipelineName) {
        cy.request('DELETE', `api/pipelines/${pipeline.id}`);
      }
    });
  });
});

Cypress.Commands.add('createPipeline', pipeline => {
  cy.request('POST', `/api/pipelines`, {
    name: pipeline.name || 'Untitled pipeline',
    rules: pipeline.rules || {},
    workerClusterName: pipeline.workerName,
    ...pipeline,
  }).then(({ body }) => body);
});

Cypress.Commands.add('createTopic', overrides => {
  cy.request('POST', '/api/topics', {
    name: makeRandomStr(),
    numberOfReplications: 1,
    numberOfPartitions: 1,
    ...overrides,
  }).then(({ body }) => body); // we'll need the returned data later on
});

Cypress.Commands.add('uploadJar', (selector, fixturePath, name, type) => {
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
