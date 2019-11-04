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
import * as wkApi from '../../src/api/workerApi';
import { createServices, deleteAllServices } from '../utils';

const generateWorker = async () => {
  const { node, broker } = await createServices({
    withBroker: true,
    withZookeeper: true,
    withNode: true,
  });
  const wkName = generate.serviceName({ prefix: 'wk' });
  const worker = {
    name: wkName,
    group: generate.serviceName({ prefix: 'group' }),
    nodeNames: [node.hostname],
    brokerClusterKey: {
      name: broker.name,
      group: broker.group,
    },
    tags: {
      name: wkName,
    },
  };
  return worker;
};

describe('Worker API', () => {
  beforeEach(() => deleteAllServices());

  it('createWorker', async () => {
    const wkCluster = await generateWorker();
    const result = await wkApi.create(wkCluster);
    const { aliveNodes, lastModified, state, error } = result;
    const {
      name,
      group,
      nodeNames,
      clientPort,
      jmxPort,
      //
      // groupId,
      // statusTopicName,
      // statusTopicPartitions,
      // statusTopicReplications,
      // configTopicName,
      // configTopicReplications,
      // offsetTopicName,
      // offsetTopicPartitions,
      // offsetTopicReplications,
      fileKeys,
      freePorts,
      imageName,
      brokerClusterKey,
      tags,
    } = result.settings;

    expect(aliveNodes).to.be.an('array');
    expect(aliveNodes).to.be.empty;

    expect(lastModified).to.be.a('number');

    expect(state).to.be.undefined;

    expect(error).to.be.undefined;

    expect(name).to.be.a('string');
    expect(name).to.eq(wkCluster.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(wkCluster.group);

    expect(nodeNames).to.be.an('array');
    expect(nodeNames).have.lengthOf(1);

    expect(clientPort).to.be.a('number');
    expect(jmxPort).to.be.a('number');

    // expect(groupId).to.be.a('string');

    // expect(statusTopicName).to.be.a('string');
    // expect(statusTopicPartitions).to.be.a('number');
    // expect(statusTopicReplications).to.be.a('number');

    // expect(configTopicName).to.be.a('string');
    // expect(configTopicReplications).to.be.a('number');

    // expect(offsetTopicName).to.be.a('string');
    // expect(offsetTopicPartitions).to.be.a('number');
    // expect(offsetTopicReplications).to.be.a('number');

    expect(fileKeys).to.be.an('array');

    expect(freePorts).to.be.an('array');

    expect(imageName).to.be.a('string');

    expect(brokerClusterKey).to.be.an('object');
    expect(brokerClusterKey).to.be.deep.eq(wkCluster.brokerClusterKey);

    expect(tags.name).to.eq(wkCluster.name);
  });

  it('fetchWorker', async () => {
    const wkCluster = await generateWorker();
    await wkApi.create(wkCluster);

    const result = await wkApi.get(wkCluster);
    const { aliveNodes, lastModified, state, error } = result;
    const {
      name,
      group,
      nodeNames,
      clientPort,
      jmxPort,
      //
      // groupId,
      // statusTopicName,
      // statusTopicPartitions,
      // statusTopicReplications,
      // configTopicName,
      // configTopicReplications,
      // offsetTopicName,
      // offsetTopicPartitions,
      // offsetTopicReplications,
      fileKeys,
      freePorts,
      imageName,
      brokerClusterKey,
      tags,
    } = result.settings;

    expect(aliveNodes).to.be.an('array');
    expect(aliveNodes).to.be.empty;

    expect(lastModified).to.be.a('number');

    expect(state).to.be.undefined;

    expect(error).to.be.undefined;

    expect(name).to.be.a('string');
    expect(name).to.eq(wkCluster.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(wkCluster.group);

    expect(nodeNames).to.be.an('array');
    expect(nodeNames).have.lengthOf(1);

    expect(clientPort).to.be.a('number');
    expect(jmxPort).to.be.a('number');

    // expect(groupId).to.be.a('string');

    // expect(statusTopicName).to.be.a('string');
    // expect(statusTopicPartitions).to.be.a('number');
    // expect(statusTopicReplications).to.be.a('number');

    // expect(configTopicName).to.be.a('string');
    // expect(configTopicReplications).to.be.a('number');

    // expect(offsetTopicName).to.be.a('string');
    // expect(offsetTopicPartitions).to.be.a('number');
    // expect(offsetTopicReplications).to.be.a('number');

    expect(fileKeys).to.be.an('array');

    expect(freePorts).to.be.an('array');

    expect(imageName).to.be.a('string');

    expect(brokerClusterKey).to.be.an('object');
    expect(brokerClusterKey).to.be.deep.eq(wkCluster.brokerClusterKey);

    expect(tags.name).to.eq(wkCluster.name);
  });

  it('fetchWorkers', async () => {
    const wkClusterOne = await generateWorker();
    const wkClusterTwo = await generateWorker();

    await wkApi.create(wkClusterOne);
    await wkApi.create(wkClusterTwo);

    const result = await wkApi.getAll();
    const workers = result.map(wk => wk.settings.name);
    expect(workers.includes(wkClusterOne.name)).to.be.true;
    expect(workers.includes(wkClusterTwo.name)).to.be.true;
  });

  it('deleteWorker', async () => {
    const wkCluster = await generateWorker();

    // delete a non-running worker
    await wkApi.create(wkCluster);
    const result = await wkApi.remove(wkCluster);

    const workers = result.map(wk => wk.settings.name);
    expect(workers.includes(wkCluster.name)).to.be.false;

    // delete a running worker
    await wkApi.create(wkCluster);
    const runningRes = await wkApi.start(wkCluster);
    expect(runningRes.state).to.eq('RUNNING');

    await wkApi.stop(wkCluster);
    await wkApi.remove(wkCluster);
  });

  it('updateWorker', async () => {
    const wkCluster = await generateWorker();
    const newParams = {
      clientPort: 2222,
      jmxPort: 3333,
    };
    const newBk = { ...wkCluster, ...newParams };

    await wkApi.create(wkCluster);

    const result = await wkApi.update(newBk);
    const { aliveNodes, lastModified, state, error } = result;
    const {
      name,
      group,
      nodeNames,
      clientPort,
      jmxPort,
      imageName,
      tags,
    } = result.settings;

    expect(aliveNodes).to.be.an('array');
    expect(aliveNodes).to.be.empty;

    expect(lastModified).to.be.a('number');

    expect(state).to.be.undefined;

    expect(error).to.be.undefined;

    expect(name).to.be.a('string');
    expect(name).to.eq(wkCluster.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(wkCluster.group);

    expect(nodeNames).to.be.an('array');
    expect(nodeNames).have.lengthOf(1);

    expect(clientPort).to.be.a('number');
    expect(clientPort).to.eq(2222);

    expect(jmxPort).to.be.a('number');
    expect(jmxPort).to.eq(3333);

    expect(imageName).to.be.a('string');

    expect(tags.name).to.eq(wkCluster.name);
  });

  it('startWorker', async () => {
    const wkCluster = await generateWorker();

    await wkApi.create(wkCluster);
    const undefinedBkRes = await wkApi.get(wkCluster);
    expect(undefinedBkRes.state).to.be.undefined;

    const runningWkRes = await wkApi.start(wkCluster);
    expect(runningWkRes.state).to.eq('RUNNING');
  });

  it('stopWorker', async () => {
    const wkCluster = await generateWorker();

    await wkApi.create(wkCluster);
    const undefinedBkRes = await wkApi.get(wkCluster);
    expect(undefinedBkRes.state).to.be.undefined;

    const runningWkRes = await wkApi.start(wkCluster);
    expect(runningWkRes.state).to.eq('RUNNING');
    expect(runningWkRes.settings.nodeNames).have.lengthOf(1);

    const stopWkRes = await wkApi.stop(wkCluster);
    expect(stopWkRes.state).to.be.undefined;

    const result = await wkApi.remove(wkCluster);
    const workers = result.map(wk => wk.settings.name);
    expect(workers.includes(wkCluster.name)).to.be.false;
  });

  it('addNodeToWorker', async () => {
    const wkCluster = await generateWorker();

    await wkApi.create(wkCluster);
    const undefinedBkRes = await wkApi.get(wkCluster);
    expect(undefinedBkRes.state).to.be.undefined;

    const runningWkRes = await wkApi.start(wkCluster);
    expect(runningWkRes.state).to.eq('RUNNING');
    expect(runningWkRes.settings.nodeNames).have.lengthOf(1);

    const { node: newNode } = await createServices({ withNode: true });
    const newParams = Object.assign({}, wkCluster, {
      nodeName: newNode.hostname,
    });
    const result = await wkApi.addNode(newParams);
    const { aliveNodes, lastModified, state, error } = result;
    const {
      name,
      group,
      nodeNames,
      clientPort,
      jmxPort,
      imageName,
      tags,
    } = result.settings;

    expect(aliveNodes).to.be.an('array');
    expect(aliveNodes.length).to.eq(2);

    expect(lastModified).to.be.a('number');

    expect(state).to.eq('RUNNING');

    expect(error).to.be.undefined;

    expect(name).to.be.a('string');
    expect(name).to.eq(wkCluster.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(wkCluster.group);

    expect(nodeNames).to.be.an('array');
    expect(nodeNames).have.lengthOf(2);
    expect(nodeNames.sort()).to.deep.eq(
      newParams.nodeNames.concat(newParams.nodeName).sort(),
    );

    expect(clientPort).to.be.a('number');
    expect(jmxPort).to.be.a('number');

    expect(imageName).to.be.a('string');

    expect(tags.name).to.eq(wkCluster.name);
  });

  it('removeNodeFromWorker', async () => {
    const wkCluster = await generateWorker();

    await wkApi.create(wkCluster);
    const undefinedBkRes = await wkApi.get(wkCluster);
    expect(undefinedBkRes.state).to.be.undefined;

    const runningWkRes = await wkApi.start(wkCluster);
    expect(runningWkRes.state).to.eq('RUNNING');
    expect(runningWkRes.settings.nodeNames).have.lengthOf(1);

    const { node: newNode } = await createServices({ withNode: true });
    const newParams = Object.assign({}, wkCluster, {
      nodeName: newNode.hostname,
    });
    const twoNodeWkData = await wkApi.addNode(newParams);
    expect(twoNodeWkData.aliveNodes).to.be.an('array');
    expect(twoNodeWkData.aliveNodes).have.lengthOf(2);
    expect(twoNodeWkData.settings.nodeNames).to.be.an('array');
    expect(twoNodeWkData.settings.nodeNames).have.lengthOf(2);

    const result = await wkApi.removeNode(newParams);
    const { lastModified, state, error } = result;
    const {
      name,
      group,
      nodeNames,
      clientPort,
      jmxPort,
      imageName,
      tags,
    } = result.settings;

    expect(lastModified).to.be.a('number');

    expect(state).to.eq('RUNNING');

    expect(error).to.be.undefined;

    expect(name).to.be.a('string');
    expect(name).to.eq(wkCluster.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(wkCluster.group);

    expect(nodeNames).to.be.an('array');
    expect(nodeNames).have.lengthOf(1);
    expect(nodeNames).to.deep.eq(newParams.nodeNames);

    expect(clientPort).to.be.a('number');
    expect(jmxPort).to.be.a('number');

    expect(imageName).to.be.a('string');

    expect(tags.name).to.eq(wkCluster.name);
  });
});
