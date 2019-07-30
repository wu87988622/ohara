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

describe('Pipeline API', () => {
  let nodeName = '';
  let zookeeperClusterName = '';
  let brokerClusterName = '';
  let workerClusterName = '';
  let pipelineName = '';

  before(() => cy.deleteAllServices());

  beforeEach(() => {
    nodeName = `node${utils.makeRandomStr()}`;
    zookeeperClusterName = `zookeeper${utils.makeRandomStr()}`;
    brokerClusterName = `broker${utils.makeRandomStr()}`;
    workerClusterName = `worker${utils.makeRandomStr()}`;
    pipelineName = `pipeline${utils.makeRandomStr()}`;

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
      zookeeperClusterName: zookeeperClusterName,
      nodeNames: [nodeName],
    });

    cy.startBroker(brokerClusterName);

    cy.testCreateWorker({
      name: workerClusterName,
      brokerClusterName,
      nodeNames: [nodeName],
    });

    cy.testCreatePipeline({
      name: pipelineName,
      workerClusterName,
    }).as('testCreatePipeline');
  });

  it('createPipeline', () => {
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
    let topicName = `topic${utils.makeRandomStr()}`;

    cy.testCreateTopic({
      name: topicName,
      brokerClusterName,
    });

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
    cy.testDeletePipeline(pipelineName).then(response => {
      expect(response.data.isSuccess).to.eq(true);
    });
  });
});
