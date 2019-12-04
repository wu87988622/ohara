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
import * as streamApi from '../../src/api/streamApi';
import * as fileApi from '../../src/api/fileApi';
import { createServices, deleteAllServices } from '../utils';

const file = {
  fixturePath: 'stream',
  // we use an existing file to simulate upload jar
  name: 'ohara-it-stream.jar',
  group: generate.serviceName({ prefix: 'group' }),
};

const generateStream = async () => {
  const { node, broker } = await createServices({
    withBroker: true,
    withZookeeper: true,
    withNode: true,
  });

  const topic = {
    name: generate.serviceName({ prefix: 'topic' }),
    group: generate.serviceName({ prefix: 'group' }),
    nodeNames: [node.hostname],
    brokerClusterKey: {
      name: broker.name,
      group: broker.group,
    },
  };
  await topicApi.create(topic);
  await topicApi.start(topic);

  const streamName = generate.serviceName({ prefix: 'stream' });
  const stream = {
    name: streamName,
    group: generate.serviceName({ prefix: 'group' }),
    nodeNames: [node.hostname],
    brokerClusterKey: {
      name: broker.name,
      group: broker.group,
    },
    jarKey: {
      name: file.name,
      group: file.group,
    },
    from: [{ name: topic.name, group: topic.group }],
    to: [{ name: topic.name, group: topic.group }],
    tags: {
      name: streamName,
    },
  };

  return stream;
};

describe('Stream API', () => {
  beforeEach(async () => {
    await deleteAllServices();
    cy.createJar(file).then(params => fileApi.create(params));
  });

  it('createStream', async () => {
    const stream = await generateStream();
    const result = await streamApi.create(stream);
    expect(result.errors).to.be.undefined;

    const { aliveNodes, lastModified, metrics, state, error } = result.data;
    const {
      name,
      group,
      nodeNames,
      brokerClusterKey,
      jmxPort,
      from,
      to,
      jarKey,
      imageName,
      tags,
    } = result.data.settings;

    expect(aliveNodes).to.be.an('array');
    expect(aliveNodes).to.be.empty;

    expect(lastModified).to.be.a('number');

    expect(metrics).to.be.an('object');
    expect(metrics.meters).to.be.a('array');

    expect(state).to.be.undefined;

    expect(error).to.be.undefined;

    expect(name).to.be.a('string');
    expect(name).to.eq(stream.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(stream.group);

    expect(nodeNames).to.be.an('array');
    expect(nodeNames).have.lengthOf(1);

    expect(jmxPort).to.be.a('number');

    expect(from).to.be.an('array');
    expect(from).to.be.lengthOf(1);
    expect(from).to.be.deep.eq(stream.from);

    expect(to).to.be.an('array');
    expect(to).to.be.lengthOf(1);
    expect(to).to.be.deep.eq(stream.to);

    expect(jarKey).to.be.an('object');
    expect(jarKey.name).to.be.eq(file.name);
    expect(jarKey.group).to.be.eq(file.group);

    expect(imageName).to.be.a('string');

    expect(brokerClusterKey).to.be.a('object');
    expect(brokerClusterKey).to.be.deep.eq(stream.brokerClusterKey);

    expect(tags.name).to.eq(stream.name);
  });

  it('fetchStream', async () => {
    const stream = await generateStream();
    await streamApi.create(stream);

    const result = await streamApi.get(stream);
    expect(result.errors).to.be.undefined;

    const { aliveNodes, lastModified, metrics, state, error } = result.data;
    const {
      name,
      group,
      nodeNames,
      brokerClusterKey,
      jmxPort,
      from,
      to,
      jarKey,
      imageName,
      tags,
    } = result.data.settings;

    expect(aliveNodes).to.be.an('array');
    expect(aliveNodes).to.be.empty;

    expect(lastModified).to.be.a('number');

    expect(metrics).to.be.an('object');
    expect(metrics.meters).to.be.a('array');

    expect(state).to.be.undefined;

    expect(error).to.be.undefined;

    expect(name).to.be.a('string');
    expect(name).to.eq(stream.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(stream.group);
    stream;
    expect(nodeNames).to.be.an('array');
    expect(nodeNames).have.lengthOf(1);

    expect(jmxPort).to.be.a('number');

    expect(from).to.be.an('array');
    expect(from).to.be.lengthOf(1);
    expect(from).to.be.deep.eq(stream.from);

    expect(to).to.be.an('array');
    expect(to).to.be.lengthOf(1);
    expect(to).to.be.deep.eq(stream.to);

    expect(jarKey).to.be.an('object');
    expect(jarKey.name).to.be.eq(file.name);
    expect(jarKey.group).to.be.eq(file.group);

    expect(imageName).to.be.a('string');

    expect(brokerClusterKey).to.be.a('object');
    expect(brokerClusterKey).to.be.deep.eq(stream.brokerClusterKey);

    expect(tags.name).to.eq(stream.name);
  });

  it('fetchStreams', async () => {
    const stream1 = await generateStream();
    const stream2 = await generateStream();

    await streamApi.create(stream1);
    await streamApi.create(stream2);

    const result = await streamApi.getAll();
    expect(result.errors).to.be.undefined;

    const streams = result.data.map(zk => zk.settings.name);
    expect(streams.includes(stream1.name)).to.be.true;
    expect(streams.includes(stream2.name)).to.be.true;
  });

  it('deleteStream', async () => {
    const stream = await generateStream();

    // delete a non-running stream
    await streamApi.create(stream);
    const result = await streamApi.remove(stream);
    expect(result.errors).to.be.undefined;

    const streams = result.data.map(stream => stream.settings.name);
    expect(streams.includes(stream.name)).to.be.false;

    // delete a running stream
    await streamApi.create(stream);
    const runningRes = await streamApi.start(stream);
    expect(runningRes.errors).to.be.undefined;
    expect(runningRes.data.state).to.eq('RUNNING');

    await streamApi.stop(stream);
    await streamApi.remove(stream);
  });

  it('updateStream', async () => {
    const stream = await generateStream();
    const newParams = {
      jmxPort: 2222,
    };
    const newStream = { ...stream, ...newParams };

    await streamApi.create(stream);

    const result = await streamApi.update(newStream);
    expect(result.errors).to.be.undefined;

    const { aliveNodes, lastModified, metrics, state, error } = result.data;
    const {
      name,
      group,
      nodeNames,
      brokerClusterKey,
      jmxPort,
      from,
      to,
      jarKey,
      imageName,
      tags,
    } = result.data.settings;

    expect(aliveNodes).to.be.an('array');
    expect(aliveNodes).to.be.empty;

    expect(lastModified).to.be.a('number');

    expect(metrics).to.be.an('object');
    expect(metrics.meters).to.be.a('array');

    expect(state).to.be.undefined;

    expect(error).to.be.undefined;

    expect(name).to.be.a('string');
    expect(name).to.eq(stream.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(stream.group);
    stream;
    expect(nodeNames).to.be.an('array');
    expect(nodeNames).have.lengthOf(1);

    expect(jmxPort).to.be.a('number');
    expect(jmxPort).to.eq(2222);

    expect(from).to.be.an('array');
    expect(from).to.be.lengthOf(1);
    expect(from).to.be.deep.eq(stream.from);

    expect(to).to.be.an('array');
    expect(to).to.be.lengthOf(1);
    expect(to).to.be.deep.eq(stream.to);

    expect(jarKey).to.be.an('object');
    expect(jarKey.name).to.be.eq(file.name);
    expect(jarKey.group).to.be.eq(file.group);

    expect(imageName).to.be.a('string');

    expect(brokerClusterKey).to.be.a('object');
    expect(brokerClusterKey).to.be.deep.eq(stream.brokerClusterKey);

    expect(tags.name).to.eq(stream.name);
  });

  it('startStream', async () => {
    const stream = await generateStream();
    await streamApi.create(stream);
    const undefinedStreamRes = await streamApi.get(stream);
    expect(undefinedStreamRes.errors).to.be.undefined;
    expect(undefinedStreamRes.data.state).to.be.undefined;

    const runningStreamRes = await streamApi.start(stream);
    expect(runningStreamRes.errors).to.be.undefined;
    expect(runningStreamRes.data.state).to.eq('RUNNING');
  });

  it('stopStream', async () => {
    const stream = await generateStream();
    await streamApi.create(stream);
    const undefinedStreamRes = await streamApi.get(stream);
    expect(undefinedStreamRes.errors).to.be.undefined;
    expect(undefinedStreamRes.data.state).to.be.undefined;

    const runningStreamRes = await streamApi.start(stream);
    expect(runningStreamRes.errors).to.be.undefined;
    expect(runningStreamRes.data.state).to.eq('RUNNING');
    expect(runningStreamRes.data.settings.nodeNames).have.lengthOf(1);

    const stopStreamRes = await streamApi.stop(stream);
    expect(stopStreamRes.errors).to.be.undefined;
    expect(stopStreamRes.data.state).to.be.undefined;

    const result = await streamApi.remove(stream);
    expect(result.errors).to.be.undefined;
    const streams = result.data.map(stream => stream.settings.name);
    expect(streams.includes(stream.name)).to.be.false;
  });
});
