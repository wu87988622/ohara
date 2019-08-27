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

import '@testing-library/cypress/add-commands';
import { isEmpty } from 'lodash';

import * as utils from '../utils';
import * as generate from '../../src/utils/generate';
import { axiosInstance } from '../../src/api/apiUtils';

// Registering service name so we can do the clean up later
// when the tests are done
Cypress.Commands.add('registerService', (serviceName, serviceType) => {
  const fileName = './services.json';
  const update = { name: serviceName, serviceType };

  cy.task('readFileMaybe', fileName).then(services => {
    // Append a new worker to the existing file
    cy.writeFile(fileName, [...services, update]);
  });
});

Cypress.Commands.add('addWorker', () => {
  const { name: nodeName } = utils.getFakeNode();
  const workerName = generate.serviceName({ prefix: 'worker' });

  // Store the worker names in a file as well as
  // in the Cypress env as we'll be using them in the tests
  Cypress.env('WORKER_NAME', workerName);
  cy.registerService(workerName, 'workers');

  cy.request('GET', 'api/brokers')
    .then(res => res.body[0])
    .as('broker');

  cy.get('@broker').then(broker => {
    cy.request('POST', 'api/workers', {
      name: workerName,
      brokerClusterName: broker.name,
      jarKeys: [],
      groupId: generate.id(),
      nodeNames: [nodeName],
    });
    cy.request('PUT', `api/workers/${workerName}/start`);
  });

  let count = 0;
  const max = 20;
  // Make a request to configurator see if worker cluster is ready for use
  const req = endPoint => {
    cy.request('GET', endPoint).then(response => {
      // Wait until the connectors are loaded in the worker we just created
      const workerIsReady = !isEmpty(response.body.connectors);

      if (workerIsReady || count > max) return;

      // if worker is not ready yet, wait a 2 sec and make another request
      count++;
      cy.wait(2000);
      req(endPoint);
    });
  };

  const endPoint = `api/workers/${workerName}`;
  cy.request('GET', endPoint).then(() => req(endPoint));
});

Cypress.Commands.add('addPipeline', pipeline => {
  cy.request('POST', `/api/pipelines`, {
    name: pipeline.name || 'Untitled pipeline',
    workerClusterName: pipeline.workerName,
    ...pipeline,
  }).then(({ body }) => body);
});

Cypress.Commands.add(
  'addTopic',
  (topicName = generate.serviceName({ prefix: 'topic' })) => {
    cy.request('GET', 'api/workers')
      .then(res => {
        // Make sure we're getting the right broker cluster name here
        const workers = res.body;
        const currentWorkerName = Cypress.env('WORKER_NAME');
        const worker = workers.find(
          worker => worker.name === currentWorkerName,
        );
        return worker.brokerClusterName;
      })
      .as('brokerClusterName');

    cy.get('@brokerClusterName').then(brokerClusterName => {
      cy.request('POST', '/api/topics', {
        name: topicName,
        numberOfReplications: 1,
        numberOfPartitions: 1,
        brokerClusterName,
      }).then(({ body }) => body);
    });

    cy.request('PUT', `/api/topics/${topicName}/start`);
    Cypress.env('TOPIC_NAME', topicName);
  },
);

Cypress.Commands.add('removeWorkers', () => {
  const fileName = './services.json';
  cy.task('readFileMaybe', fileName).then(services => {
    if (!services) return; // File is not there, skip the whole process

    const workerNames = services
      .filter(service => service.serviceType === 'workers')
      .map(worker => worker.name);

    cy.request('GET', 'api/workers').then(res => {
      const targetWorkers = res.body.filter(worker =>
        workerNames.includes(worker.name),
      );

      if (targetWorkers.length > 0) {
        targetWorkers.forEach(targetWorker => {
          const { name } = targetWorker;

          cy.request('PUT', `api/workers/${name}/stop`);

          let count = 0;
          const max = 10;
          // Make a request to configurator see if worker cluster is ready for use
          const req = endPoint => {
            cy.request('GET', endPoint).then(res => {
              // When connectors field has the right connector info
              // this means that everything is ready to be tested
              const workerIsReady = res.body.state === undefined;

              if (workerIsReady || count > max) return;

              // if worker is not ready yet, wait a 2 sec and make another request
              count++;
              cy.wait(2000);
              req(endPoint);
            });
          };

          const endPoint = `api/workers/${name}`;
          cy.request('GET', endPoint).then(() => req(endPoint));

          cy.request('DELETE', `api/workers/${name}`).then(() =>
            utils.recursiveDeleteWorker('api/workers', name),
          );
        });
      }
    });
  });
});

Cypress.Commands.add('removeTopic', topicName => {
  cy.request('GET', 'api/topics').then(res => {
    res.body.forEach(({ name }) => {
      if (name === topicName) {
        cy.request('PUT', `/api/topics/${name}/stop`);
        cy.request('DELETE', `api/topics/${name}`);
      }
    });
  });
});

Cypress.Commands.add('removePipeline', pipelineName => {
  cy.request('GET', 'api/pipelines').then(res => {
    res.body.forEach(({ name, id }) => {
      if (name === pipelineName) {
        cy.request('DELETE', `api/pipelines/${id}`);
      }
    });
  });
});

Cypress.Commands.add('uploadStreamAppJar', () => {
  cy.getByTestId('toolbar-streams')
    .click()
    .uploadJar(
      'input[type=file]',
      'streamApp/ohara-streamapp.jar',
      'ohara-streamapp.jar',
      'application/java-archive',
    )
    .wait(500);
});

Cypress.Commands.add('uploadTestStreamAppJar', workerClusterName => {
  cy.fixture(`streamApp/ohara-streamapp.jar`, 'base64')
    .then(Cypress.Blob.base64StringToBlob)
    .then(blob => {
      const type = 'application/java-archive';
      const url = '/api/files';
      const config = {
        headers: {
          'content-type': 'multipart/form-data',
        },
      };
      const testFile = new File([blob], 'ohara-streamapp.jar', { type });
      const dataTransfer = new DataTransfer();
      dataTransfer.items.add(testFile);
      blob = dataTransfer.files;

      let formData = new FormData();
      formData.append('file', blob[0]);
      formData.append('group', workerClusterName);
      const res = axiosInstance.post(url, formData, config);
      cy.log(res);
    });
});

Cypress.Commands.add('uploadJar', (selector, fixturePath, name, type) => {
  cy.get(selector).then(subject =>
    cy.window().then(win =>
      cy
        .fixture(fixturePath, 'base64')
        .then(Cypress.Blob.base64StringToBlob)
        .then(blob => {
          const el = subject[0];
          const testFile = new win.File([blob], name, {
            type,
          });
          const dataTransfer = new win.DataTransfer();
          dataTransfer.items.add(testFile);
          el.files = dataTransfer.files;
          cy.wrap(subject).trigger('change', { force: true });
        }),
    ),
  );
});
