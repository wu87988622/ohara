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
/* eslint-disable @typescript-eslint/no-unused-expressions */
// eslint is complaining about `expect(thing).to.be.undefined`

import * as generate from '../../src/utils/generate';
import * as wkApi from '../../src/api/workerApi';
import { createServices, deleteAllServices } from '../utils';
import { SERVICE_STATE } from '../../src/api/apiInterface/clusterInterface';

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

    const { aliveNodes, lastModified, state, error } = result.data;
    const {
      name,
      group,
      nodeNames,
      clientPort,
      jmxPort,
      group__id,
      status__storage__topic,
      status__storage__partitions,
      status__storage__replication__factor,
      config__storage__topic,
      config__storage__replication__factor,
      offset__storage__topic,
      offset__storage__partitions,
      offset__storage__replication__factor,
      pluginKeys,
      freePorts,
      imageName,
      brokerClusterKey,
      tags,
    } = result.data;

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

    expect(group__id).to.be.a('string');

    expect(status__storage__topic).to.be.a('string');
    expect(status__storage__partitions).to.be.a('number');
    expect(status__storage__replication__factor).to.be.a('number');

    expect(config__storage__topic).to.be.a('string');
    expect(config__storage__replication__factor).to.be.a('number');

    expect(offset__storage__topic).to.be.a('string');
    expect(offset__storage__partitions).to.be.a('number');
    expect(offset__storage__replication__factor).to.be.a('number');

    expect(pluginKeys).to.be.an('array');

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

    const { aliveNodes, lastModified, state, error } = result.data;
    const {
      name,
      group,
      nodeNames,
      clientPort,
      jmxPort,
      group__id,
      status__storage__topic,
      status__storage__partitions,
      status__storage__replication__factor,
      config__storage__topic,
      config__storage__replication__factor,
      offset__storage__topic,
      offset__storage__partitions,
      offset__storage__replication__factor,
      pluginKeys,
      freePorts,
      imageName,
      brokerClusterKey,
      tags,
    } = result.data;

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

    expect(group__id).to.be.a('string');

    expect(status__storage__topic).to.be.a('string');
    expect(status__storage__partitions).to.be.a('number');
    expect(status__storage__replication__factor).to.be.a('number');

    expect(config__storage__topic).to.be.a('string');
    expect(config__storage__replication__factor).to.be.a('number');

    expect(offset__storage__topic).to.be.a('string');
    expect(offset__storage__partitions).to.be.a('number');
    expect(offset__storage__replication__factor).to.be.a('number');

    expect(pluginKeys).to.be.an('array');

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

    const workers = result.data.map(wk => wk.name);
    expect(workers.includes(wkClusterOne.name)).to.be.true;
    expect(workers.includes(wkClusterTwo.name)).to.be.true;
  });

  it('deleteWorker', async () => {
    const wkCluster = await generateWorker();

    // delete a non-running worker
    await wkApi.create(wkCluster);
    await wkApi.remove(wkCluster);
    const result = await wkApi.getAll();

    const workers = result.data.map(wk => wk.name);
    expect(workers.includes(wkCluster.name)).to.be.false;

    // delete a running worker
    await wkApi.create(wkCluster);
    await wkApi.start(wkCluster);
    const runningRes = await wkApi.get(wkCluster);
    expect(runningRes.data.state).to.eq(SERVICE_STATE.RUNNING);

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

    const { aliveNodes, lastModified, state, error } = result.data;
    const {
      name,
      group,
      nodeNames,
      clientPort,
      jmxPort,
      imageName,
      tags,
    } = result.data;

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
    expect(undefinedBkRes.data.state).to.be.undefined;

    await wkApi.start(wkCluster);
    const runningWkRes = await wkApi.get(wkCluster);
    expect(runningWkRes.data.state).to.eq(SERVICE_STATE.RUNNING);
  });

  it('stopWorker', async () => {
    const wkCluster = await generateWorker();

    await wkApi.create(wkCluster);
    const undefinedBkRes = await wkApi.get(wkCluster);
    expect(undefinedBkRes.data.state).to.be.undefined;

    await wkApi.start(wkCluster);
    const runningWkRes = await wkApi.get(wkCluster);
    expect(runningWkRes.data.state).to.eq(SERVICE_STATE.RUNNING);
    expect(runningWkRes.data.nodeNames).have.lengthOf(1);

    await wkApi.stop(wkCluster);
    const stopWkRes = await wkApi.get(wkCluster);
    expect(stopWkRes.data.state).to.be.undefined;

    await wkApi.remove(wkCluster);
    const result = await wkApi.getAll();

    const workers = result.data.map(wk => wk.name);
    expect(workers.includes(wkCluster.name)).to.be.false;
  });

  it('addNodeToWorker', async () => {
    const wkCluster = await generateWorker();

    await wkApi.create(wkCluster);
    const undefinedBkRes = await wkApi.get(wkCluster);
    expect(undefinedBkRes.data.state).to.be.undefined;

    await wkApi.start(wkCluster);
    const runningWkRes = await wkApi.get(wkCluster);
    expect(runningWkRes.data.state).to.eq(SERVICE_STATE.RUNNING);
    expect(runningWkRes.data.nodeNames).have.lengthOf(1);

    const { node: newNode } = await createServices({ withNode: true });
    await wkApi.addNode(wkCluster, newNode.hostname);
    const result = await wkApi.get(wkCluster);

    const { aliveNodes, lastModified, state, error } = result.data;
    const {
      name,
      group,
      nodeNames,
      clientPort,
      jmxPort,
      imageName,
      tags,
    } = result.data;

    expect(aliveNodes).to.be.an('array');
    expect(aliveNodes.length).to.eq(2);

    expect(lastModified).to.be.a('number');

    expect(state).to.eq(SERVICE_STATE.RUNNING);

    expect(error).to.be.undefined;

    expect(name).to.be.a('string');
    expect(name).to.eq(wkCluster.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(wkCluster.group);

    expect(nodeNames).to.be.an('array');
    expect(nodeNames).have.lengthOf(2);
    expect(nodeNames.sort()).to.deep.eq(
      wkCluster.nodeNames.concat(newNode.hostname).sort(),
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
    expect(undefinedBkRes.data.state).to.be.undefined;

    await wkApi.start(wkCluster);
    const runningWkRes = await wkApi.get(wkCluster);
    expect(runningWkRes.data.state).to.eq(SERVICE_STATE.RUNNING);
    expect(runningWkRes.data.nodeNames).have.lengthOf(1);

    const { node: newNode } = await createServices({ withNode: true });
    await wkApi.addNode(wkCluster, newNode.hostname);
    const twoNodeWkData = await wkApi.get(wkCluster);

    expect(twoNodeWkData.data.aliveNodes).to.be.an('array');
    expect(twoNodeWkData.data.aliveNodes).have.lengthOf(2);
    expect(twoNodeWkData.data.nodeNames).to.be.an('array');
    expect(twoNodeWkData.data.nodeNames).have.lengthOf(2);

    await wkApi.removeNode(wkCluster, newNode.hostname);
    const result = await wkApi.get(wkCluster);

    const { lastModified, state, error } = result.data;
    const {
      name,
      group,
      nodeNames,
      clientPort,
      jmxPort,
      imageName,
      tags,
    } = result.data;

    expect(lastModified).to.be.a('number');

    expect(state).to.eq(SERVICE_STATE.RUNNING);

    expect(error).to.be.undefined;

    expect(name).to.be.a('string');
    expect(name).to.eq(wkCluster.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(wkCluster.group);

    expect(nodeNames).to.be.an('array');
    expect(nodeNames).have.lengthOf(1);
    expect(nodeNames).to.deep.eq(wkCluster.nodeNames);

    expect(clientPort).to.be.a('number');
    expect(jmxPort).to.be.a('number');

    expect(imageName).to.be.a('string');

    expect(tags.name).to.eq(wkCluster.name);
  });
});
