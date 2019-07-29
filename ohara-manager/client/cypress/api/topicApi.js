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

describe('Topic API', () => {
  let nodeName = '';
  let zookeeperClusterName = '';
  let brokerClusterName = '';
  let topicName = '';

  before(() => cy.deleteAllServices());

  beforeEach(() => {
    nodeName = `node${utils.makeRandomStr()}`;
    zookeeperClusterName = `zookeeper${utils.makeRandomStr()}`;
    brokerClusterName = `broker${utils.makeRandomStr()}`;
    topicName = `topic${utils.makeRandomStr()}`;

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
      zookeeperClusterName,
      nodeNames: [nodeName],
    });

    cy.startBroker(brokerClusterName);

    cy.testCreateTopic({
      name: topicName,
      brokerClusterName,
    }).as('testCreateTopic');
  });

  it('CreateTopic', () => {
    cy.get('@testCreateTopic').then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const {
        name,
        numberOfPartitions,
        numberOfReplications,
        metrics,
      } = result;

      expect(isSuccess).to.eq(true);

      expect(name).to.eq(topicName);
      expect(numberOfPartitions).to.be.a('number');
      expect(numberOfReplications).to.be.a('number');
      expect(metrics).to.be.a('object');
      expect(metrics.meters).to.be.a('array');
    });
  });

  it('fetchTopic', () => {
    cy.fetchTopic(topicName).then(res => {
      const {
        data: { isSuccess, result },
      } = res;

      const {
        name,
        numberOfPartitions,
        numberOfReplications,
        metrics,
      } = result;

      expect(isSuccess).to.eq(true);

      expect(name).to.eq(topicName);
      expect(numberOfPartitions).to.be.a('number');
      expect(numberOfReplications).to.be.a('number');
      expect(metrics).to.be.a('object');
      expect(metrics.meters).to.be.a('array');
    });
  });

  it('fetchTopics', () => {
    const paramsOne = {
      name: utils.makeRandomStr(),
      brokerClusterName,
    };

    const paramsTwo = {
      name: utils.makeRandomStr(),
      brokerClusterName,
    };

    cy.testCreateTopic(paramsOne);
    cy.testCreateTopic(paramsTwo);

    cy.fetchTopics().then(res => {
      const {
        data: { isSuccess, result },
      } = res;

      expect(isSuccess).to.eq(true);

      const topics = result.filter(
        topic => topic.name === paramsOne.name || topic.name === paramsTwo.name,
      );

      expect(topics.length).to.eq(2);
    });
  });
});
