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

/* eslint-disable no-unused-expressions */
// eslint is complaining about `expect(thing).to.be.undefined`

import * as generate from '../../src/utils/generate';
import * as topicApi from '../../src/api/topicApi';
import { createServices, deleteAllServices } from '../utils';

const generateTopic = async () => {
  const { node, broker } = await createServices({
    withBroker: true,
    withZookeeper: true,
    withNode: true,
  });
  const topicName = generate.serviceName({ prefix: 'topic' });
  const topic = {
    name: topicName,
    group: generate.serviceName({ prefix: 'group' }),
    nodeNames: [node.hostname],
    brokerClusterKey: {
      name: broker.name,
      group: broker.group,
    },
    tags: {
      name: topicName,
    },
  };
  return topic;
};

describe('Topic API', () => {
  beforeEach(() => deleteAllServices());

  it('createTopic', async () => {
    const topic = await generateTopic();
    const result = await topicApi.create(topic);

    const { partitionInfos, metrics, state, lastModified } = result;
    const {
      brokerClusterKey,
      numberOfPartitions,
      numberOfReplications,
      group,
      name,
      tags,
    } = result.settings;

    expect(partitionInfos).to.be.an('array');

    expect(metrics).to.be.an('object');
    expect(metrics.meters).to.be.an('array');

    expect(state).to.be.undefined;

    expect(lastModified).to.be.a('number');

    expect(brokerClusterKey).to.be.an('object');
    expect(brokerClusterKey).to.be.deep.eq(topic.brokerClusterKey);

    expect(numberOfPartitions).to.be.a('number');

    expect(numberOfReplications).to.be.a('number');

    expect(name).to.be.a('string');
    expect(name).to.eq(topic.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(topic.group);

    expect(tags.name).to.eq(topic.name);
  });

  it('fetchTopic', async () => {
    const topic = await generateTopic();
    await topicApi.create(topic);

    const result = await topicApi.get(topic);

    const { partitionInfos, metrics, state, lastModified } = result;
    const {
      brokerClusterKey,
      numberOfPartitions,
      numberOfReplications,
      group,
      name,
      tags,
    } = result.settings;

    expect(partitionInfos).to.be.an('array');

    expect(metrics).to.be.an('object');
    expect(metrics.meters).to.be.an('array');

    expect(state).to.be.undefined;

    expect(lastModified).to.be.a('number');

    expect(brokerClusterKey).to.be.an('object');
    expect(brokerClusterKey).to.be.deep.eq(topic.brokerClusterKey);

    expect(numberOfPartitions).to.be.a('number');

    expect(numberOfReplications).to.be.a('number');

    expect(name).to.be.a('string');
    expect(name).to.eq(topic.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(topic.group);

    expect(tags.name).to.eq(topic.name);
  });

  it('fetchTopics', async () => {
    const topic1 = await generateTopic();
    const topic2 = await generateTopic();
    await topicApi.create(topic1);
    await topicApi.create(topic2);

    const result = await topicApi.getAll();
    const topics = result.map(topic => topic.settings.name);
    expect(topics.includes(topic1.name)).to.be.true;
    expect(topics.includes(topic2.name)).to.be.true;
  });

  it('deleteTopic', async () => {
    const topic = await generateTopic();

    // delete a non-running topic
    await topicApi.create(topic);
    const result = await topicApi.remove(topic);
    const topics = result.map(topic => topic.settings.name);
    expect(topics.includes(topic.name)).to.be.false;

    // delete a running topic
    await topicApi.create(topic);
    const runningRes = await topicApi.start(topic);
    expect(runningRes.state).to.eq('RUNNING');

    await topicApi.stop(topic);
    await topicApi.remove(topic);
  });

  it('updateTopic', async () => {
    const topic = await generateTopic();
    const newParams = {
      numberOfReplications: 5,
      numberOfPartitions: 3,
    };
    const newTopic = { ...topic, ...newParams };

    await topicApi.create(topic);
    const result = await topicApi.update(newTopic);

    const { partitionInfos, metrics, state, lastModified } = result;
    const {
      brokerClusterKey,
      numberOfPartitions,
      numberOfReplications,
      group,
      name,
      tags,
    } = result.settings;

    expect(partitionInfos).to.be.an('array');

    expect(metrics).to.be.an('object');
    expect(metrics.meters).to.be.an('array');

    expect(state).to.be.undefined;

    expect(lastModified).to.be.a('number');

    expect(brokerClusterKey).to.be.an('object');
    expect(brokerClusterKey).to.be.deep.eq(topic.brokerClusterKey);

    expect(numberOfPartitions).to.be.a('number');
    expect(numberOfPartitions).to.eq(newTopic.numberOfPartitions);

    expect(numberOfReplications).to.be.a('number');
    expect(numberOfReplications).to.eq(newTopic.numberOfReplications);

    expect(name).to.be.a('string');
    expect(name).to.eq(topic.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(topic.group);

    expect(tags.name).to.eq(topic.name);
  });

  it('startTopic', async () => {
    const topic = await generateTopic();

    await topicApi.create(topic);
    const undefinedTopicRes = await topicApi.get(topic);
    expect(undefinedTopicRes.state).to.be.undefined;

    const runningTopicRes = await topicApi.start(topic);
    expect(runningTopicRes.state).to.eq('RUNNING');
  });

  it('stopTopic', async () => {
    const topic = await generateTopic();

    await topicApi.create(topic);
    const undefinedTopicRes = await topicApi.get(topic);
    expect(undefinedTopicRes.state).to.be.undefined;

    const runningTopicRes = await topicApi.start(topic);
    expect(runningTopicRes.state).to.eq('RUNNING');

    const stopTopicRes = await topicApi.stop(topic);
    expect(stopTopicRes.state).to.be.undefined;

    const result = await topicApi.remove(topic);
    const topics = result.map(topic => topic.settings.name);
    expect(topics.includes(topic.name)).to.be.false;
  });
});
