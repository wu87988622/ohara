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

import * as generate from '../../src/utils/generate';

const setup = () => {
  const nodeName = generate.serviceName({ prefix: 'node' });
  const zookeeperClusterName = generate.serviceName({ prefix: 'zookeeper' });
  const brokerClusterName = generate.serviceName({ prefix: 'broker' });
  const workerClusterName = generate.serviceName({ prefix: 'worker' });
  const pipelineName = generate.serviceName({ prefix: 'pipeline' });

  cy.createNode({
    name: nodeName,
    port: generate.port(),
    user: generate.userName(),
    password: generate.password(),
  });

  cy.createZookeeper({
    name: zookeeperClusterName,
    nodeNames: [nodeName],
  });

  cy.startZookeeper(zookeeperClusterName);

  cy.createBroker({
    name: brokerClusterName,
    nodeNames: [nodeName],
    zookeeperClusterName,
  });

  cy.startBroker(brokerClusterName);

  cy.testCreateWorker({
    name: workerClusterName,
    nodeNames: [nodeName],
    brokerClusterName,
  });

  cy.testCreatePipeline({
    name: pipelineName,
    tags: {
      workerClusterName,
    },
  }).as('testCreatePipeline');

  return {
    zookeeperClusterName,
    brokerClusterName,
    workerClusterName,
    pipelineName,
  };
};

describe('Pipeline API', () => {
  beforeEach(() => cy.deleteAllServices());

  it('createPipeline', () => {
    const { pipelineName, workerClusterName } = setup();

    cy.get('@testCreatePipeline').then(response => {
      const {
        data: { isSuccess, result },
      } = response;

      expect(isSuccess).to.eq(true);

      expect(result.name).to.eq(pipelineName);
      expect(result.tags.workerClusterName).to.eq(workerClusterName);
      expect(result.objects).to.be.an('array');
      expect(result.flows).to.be.an('array');
    });
  });

  it('fetchPipeline', () => {
    const { pipelineName, workerClusterName } = setup();

    cy.fetchPipeline(pipelineName).then(response => {
      const {
        data: { isSuccess, result },
      } = response;

      expect(isSuccess).to.eq(true);

      expect(result.name).to.eq(pipelineName);
      expect(result.tags.workerClusterName).to.eq(workerClusterName);
      expect(result.objects).to.be.an('array');
      expect(result.flows).to.be.an('array');
    });
  });

  it('fetchPipelines', () => {
    const { workerClusterName } = setup();

    const paramsOne = {
      name: generate.serviceName({ prefix: 'pipeline' }),
      workerClusterName,
    };

    const paramsTwo = {
      name: generate.serviceName({ prefix: 'pipeline' }),
      workerClusterName,
    };

    cy.testCreatePipeline(paramsOne);
    cy.testCreatePipeline(paramsTwo);

    cy.fetchPipelines().then(response => {
      const {
        data: { isSuccess, result },
      } = response;

      expect(isSuccess).to.eq(true);

      const pipelines = result.filter(
        pipeline =>
          pipeline.name === paramsOne.name || pipeline.name === paramsTwo.name,
      );

      expect(pipelines.length).to.eq(2);
    });
  });

  it('updatePipeline', () => {
    const { brokerClusterName, workerClusterName, pipelineName } = setup();

    let topicName = generate.serviceName({ prefix: 'topic' });

    cy.testCreateTopic({
      name: topicName,
      brokerClusterName,
    });

    cy.startTopic(topicName);

    const params = {
      name: pipelineName,
      params: {
        flows: [{ from: { group: 'default', name: topicName }, to: [] }],
        workerClusterName,
      },
    };

    cy.updatePipeline(params).then(response => {
      const {
        data: { isSuccess, result },
      } = response;

      expect(isSuccess).to.eq(true);

      expect(result.name).to.eq(pipelineName);
      expect(result.tags.workerClusterName).to.eq(workerClusterName);
      expect(result.objects).to.be.an('array');
      expect(result.flows).to.be.an('array');

      const [topic] = result.objects;

      expect(topic.kind).to.eq('topic');
      expect(topic.name).to.eq(topicName);
      expect(topic.metrics).to.be.an('object');
      expect(topic.metrics.meters).to.be.an('array');
    });
  });

  it('deletePipeline', () => {
    const { pipelineName } = setup();

    cy.testDeletePipeline(pipelineName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });
  });
});
