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

import * as workerApi from '../../src/api/workerApi';
import * as topicApi from '../../src/api/topicApi';
import * as nodeApi from '../../src/api/nodeApi';
import * as pipelineApi from '../../src/api/pipelineApi';
import * as connectorApi from '../../src/api/connectorApi';
import * as streamApp from '../../src/api/streamApi';
import { axiosInstance } from '../../src/api/apiUtils';
import * as zookeeperApi from '../../src/api/zookeeperApi';
import * as brokerApi from '../../src/api/brokerApi';
import { fetchJars } from '../../src/api/jarApi';
import { fetchLogs } from '../../src/api/logApi';
import { validateConnector } from '../../src/api/validateApi';
import { fetchContainers } from '../../src/api/containerApi';

Cypress.Commands.add('fetchContainers', name => fetchContainers(name));

Cypress.Commands.add('validateConnector', params => validateConnector(params));

Cypress.Commands.add('deleteProperty', params =>
  streamApp.deleteProperty(params),
);

Cypress.Commands.add('stopStreamApp', id => streamApp.stopStreamApp(id));

Cypress.Commands.add('updateProperty', params =>
  streamApp.updateProperty(params),
);

Cypress.Commands.add('fetchProperty', id => streamApp.fetchProperty(id));

Cypress.Commands.add('createProperty', params =>
  streamApp.createProperty(params),
);

Cypress.Commands.add('deleteConnector', id => connectorApi.deleteConnector(id));

Cypress.Commands.add('stopConnector', id => connectorApi.stopConnector(id));

Cypress.Commands.add('startConnector', id => connectorApi.startConnector(id));

Cypress.Commands.add('updateConnector', params =>
  connectorApi.updateConnector(params),
);

Cypress.Commands.add('fetchConnector', id => connectorApi.fetchConnector(id));

Cypress.Commands.add('createConnector', params =>
  connectorApi.createConnector(params),
);

Cypress.Commands.add('fetchLogs', (serviceName, clusterName) =>
  fetchLogs(serviceName, clusterName),
);

Cypress.Commands.add('deleteStreamAppJar', name => streamApp.deleteJar(name));

Cypress.Commands.add('fetchStreamAppJars', wk => streamApp.fetchJars(wk));

Cypress.Commands.add('testUploadStreamAppJar', params => {
  const { jarName, wk } = params;
  cy.fixture(`streamApp/${jarName}`, 'base64')
    .then(Cypress.Blob.base64StringToBlob)
    .then(blob => {
      const type = 'application/java-archive';
      const url = '/api/stream/jars';
      const config = {
        headers: {
          'content-type': 'multipart/form-data',
        },
      };
      const testFile = new File([blob], jarName, { type: type });
      const dataTransfer = new DataTransfer();
      dataTransfer.items.add(testFile);
      blob = dataTransfer.files;
      let formData = new FormData();
      formData.append('streamapp', blob[0]);
      formData.append('cluster', wk);
      return axiosInstance.post(url, formData, config);
    });
});
Cypress.Commands.add('testDeletePipeline', id =>
  pipelineApi.deletePipeline(id),
);

Cypress.Commands.add('updatePipeline', params =>
  pipelineApi.updatePipeline(params),
);

Cypress.Commands.add('fetchPipelines', () => pipelineApi.fetchPipelines());

Cypress.Commands.add('fetchPipeline', id => pipelineApi.fetchPipeline(id));

Cypress.Commands.add('testCreatePipeline', params =>
  pipelineApi.createPipeline(params),
);

Cypress.Commands.add('fetchNodes', () => nodeApi.fetchNodes());

Cypress.Commands.add('updateNode', params => nodeApi.updateNode(params));

Cypress.Commands.add('createNode', params => nodeApi.createNode(params));

Cypress.Commands.add('fetchTopics', () => topicApi.fetchTopics());

Cypress.Commands.add('fetchTopic', name => topicApi.fetchTopic(name));

Cypress.Commands.add('testCreateTopic', params => topicApi.createTopic(params));

Cypress.Commands.add('fetchJars', group => fetchJars(group));

Cypress.Commands.add('createJar', jarName => {
  cy.fixture(`plugin/${jarName}`, 'base64')
    .then(Cypress.Blob.base64StringToBlob)
    .then(blob => {
      const type = 'application/java-archive';
      const url = '/api/jars';
      const config = {
        headers: {
          'content-type': 'multipart/form-data',
        },
      };
      const testFile = new File([blob], jarName, { type: type });
      const dataTransfer = new DataTransfer();
      dataTransfer.items.add(testFile);
      blob = dataTransfer.files;
      let formData = new FormData();
      formData.append('jar', blob[0]);
      return axiosInstance.post(url, formData, config);
    });
});

Cypress.Commands.add('fetchWorker', name => workerApi.fetchWorker(name));

Cypress.Commands.add('fetchWorkers', () => workerApi.fetchWorkers());

Cypress.Commands.add('testCreateWorker', params =>
  workerApi.createWorker(params),
);

Cypress.Commands.add('createBroker', params => brokerApi.createBroker(params));
Cypress.Commands.add('fetchBrokers', () => brokerApi.fetchBrokers());
Cypress.Commands.add('fetchBroker', brokerName =>
  brokerApi.fetchBroker(brokerName),
);
Cypress.Commands.add('startBroker', brokerName =>
  brokerApi.startBroker(brokerName),
);
Cypress.Commands.add('stopBroker', brokerName =>
  brokerApi.stopBroker(brokerName),
);
Cypress.Commands.add('deleteBroker', brokerName =>
  brokerApi.deleteBroker(brokerName),
);

Cypress.Commands.add('fetchZookeeper', name =>
  zookeeperApi.fetchZookeeper(name),
);

Cypress.Commands.add('fetchZookeepers', () => zookeeperApi.fetchZookeepers());

Cypress.Commands.add('deleteZookeeper', name =>
  zookeeperApi.deleteZookeeper(name),
);

Cypress.Commands.add('stopZookeeper', name => zookeeperApi.stopZookeeper(name));

Cypress.Commands.add('startZookeeper', name =>
  zookeeperApi.startZookeeper(name),
);

Cypress.Commands.add('createZookeeper', params =>
  zookeeperApi.createZookeeper(params),
);
