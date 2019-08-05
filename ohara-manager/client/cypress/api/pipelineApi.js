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

import * as utils from '../utils';

const setup = () => {
  const nodeName = `node${utils.makeRandomStr()}`;
  const zookeeperClusterName = `zookeeper${utils.makeRandomStr()}`;
  const brokerClusterName = `broker${utils.makeRandomStr()}`;
  const workerClusterName = `worker${utils.makeRandomStr()}`;
  const pipelineName = `pipeline${utils.makeRandomStr()}`;

  cy.createNode({
    name: nodeName,
    port: 22,
    user: utils.makeRandomStr(),
    password: utils.makeRandomStr(),
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
    workerClusterName,
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
    const { pipelineName } = setup();

    cy.get('@testCreatePipeline').then(response => {
      const {
        data: { isSuccess, result },
      } = response;
      const { name, workerClusterName, objects, rules } = result;

      expect(isSuccess).to.eq(true);

      expect(name).to.eq(pipelineName);
      expect(workerClusterName).to.be.a('string');
      expect(objects).to.be.an('array');
      expect(rules).to.be.an('object');
    });
  });

  it('fetchPipeline', () => {
    const { pipelineName } = setup();

    cy.fetchPipeline(pipelineName).then(response => {
      const {
        data: { isSuccess, result },
      } = response;
      const { name, workerClusterName, objects, rules } = result;

      expect(isSuccess).to.eq(true);

      expect(name).to.eq(pipelineName);
      expect(workerClusterName).to.be.a('string');
      expect(objects).to.be.a('array');
      expect(rules).to.be.an('object');
    });
  });

  it('fetchPipelines', () => {
    const { workerClusterName } = setup();

    const paramsOne = {
      name: `pipeline${utils.makeRandomStr()}`,
      workerClusterName,
    };

    const paramsTwo = {
      name: `pipeline${utils.makeRandomStr()}`,
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

    let topicName = `topic${utils.makeRandomStr()}`;

    cy.testCreateTopic({
      name: topicName,
      brokerClusterName,
    });

    cy.startTopic(topicName);

    const params = {
      name: pipelineName,
      params: {
        rules: {
          [topicName]: [],
        },
        workerClusterName,
      },
    };

    cy.updatePipeline(params).then(response => {
      const {
        data: { isSuccess, result },
      } = response;

      expect(isSuccess).to.eq(true);

      expect(result.name).to.eq(pipelineName);
      expect(result.workerClusterName).to.eq(workerClusterName);
      expect(result.objects).to.be.a('array');
      expect(result.rules).to.be.a('object');

      expect(result.objects[0].kind).to.eq('topic');
      expect(result.objects[0].name).to.eq(topicName);
      expect(result.objects[0].metrics).to.be.an('object');
      expect(result.objects[0].metrics.meters).to.be.an('array');
    });
  });

  it('deletePipeline', () => {
    const { pipelineName } = setup();

    cy.testDeletePipeline(pipelineName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });
  });
});
