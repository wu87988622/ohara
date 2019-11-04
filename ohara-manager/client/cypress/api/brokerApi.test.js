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
import * as bkApi from '../../src/api/brokerApi';
import { createServices, deleteAllServices } from '../utils';

const generateBroker = async () => {
  const { node, zookeeper } = await createServices({
    withZookeeper: true,
    withNode: true,
  });
  const bkName = generate.serviceName({ prefix: 'bk' });
  const broker = {
    name: bkName,
    group: generate.serviceName({ prefix: 'group' }),
    nodeNames: [node.hostname],
    zookeeperClusterKey: {
      name: zookeeper.name,
      group: zookeeper.group,
    },
    tags: {
      name: bkName,
    },
  };
  return broker;
};

describe('Broker API', () => {
  beforeEach(() => deleteAllServices());

  it('createBroker', async () => {
    const bkCluster = await generateBroker();
    const result = await bkApi.create(bkCluster);
    const { aliveNodes, lastModified, state, error } = result;
    const {
      name,
      group,
      nodeNames,
      clientPort,
      jmxPort,
      imageName,
      zookeeperClusterKey,
      tags,
    } = result.settings;

    expect(aliveNodes).to.be.an('array');
    expect(aliveNodes).to.be.empty;

    expect(lastModified).to.be.a('number');

    expect(state).to.be.undefined;

    expect(error).to.be.undefined;

    expect(name).to.be.a('string');
    expect(name).to.eq(bkCluster.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(bkCluster.group);

    expect(nodeNames).to.be.an('array');
    expect(nodeNames).have.lengthOf(1);

    expect(clientPort).to.be.a('number');
    expect(jmxPort).to.be.a('number');

    expect(imageName).to.be.a('string');

    expect(zookeeperClusterKey).to.be.an('object');
    expect(zookeeperClusterKey).to.be.deep.eq(bkCluster.zookeeperClusterKey);

    expect(tags.name).to.eq(bkCluster.name);
  });

  it('fetchBroker', async () => {
    const bkCluster = await generateBroker();
    await bkApi.create(bkCluster);

    const result = await bkApi.get(bkCluster);
    const { aliveNodes, lastModified, state, error } = result;
    const {
      name,
      group,
      nodeNames,
      clientPort,
      jmxPort,
      imageName,
      zookeeperClusterKey,
      tags,
    } = result.settings;

    expect(aliveNodes).to.be.an('array');
    expect(aliveNodes).to.be.empty;

    expect(lastModified).to.be.a('number');

    expect(state).to.be.undefined;

    expect(error).to.be.undefined;

    expect(name).to.be.a('string');
    expect(name).to.eq(bkCluster.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(bkCluster.group);

    expect(nodeNames).to.be.an('array');
    expect(nodeNames).have.lengthOf(1);

    expect(clientPort).to.be.a('number');
    expect(jmxPort).to.be.a('number');

    expect(imageName).to.be.a('string');

    expect(zookeeperClusterKey).to.be.an('object');
    expect(zookeeperClusterKey).to.be.deep.eq(bkCluster.zookeeperClusterKey);

    expect(tags.name).to.eq(bkCluster.name);
  });

  it('fetchBrokers', async () => {
    const bkClusterOne = await generateBroker();
    const bkClusterTwo = await generateBroker();

    await bkApi.create(bkClusterOne);
    await bkApi.create(bkClusterTwo);

    const result = await bkApi.getAll();
    const brokers = result.map(bk => bk.settings.name);
    expect(brokers.includes(bkClusterOne.name)).to.be.true;
    expect(brokers.includes(bkClusterTwo.name)).to.be.true;
  });

  it('deleteBroker', async () => {
    const bkCluster = await generateBroker();

    // delete a non-running broker
    await bkApi.create(bkCluster);
    const result = await bkApi.remove(bkCluster);
    const brokers = result.map(bk => bk.settings.name);
    expect(brokers.includes(bkCluster.name)).to.be.false;

    // delete a running broker
    await bkApi.create(bkCluster);
    const runningRes = await bkApi.start(bkCluster);
    expect(runningRes.state).to.eq('RUNNING');

    await bkApi.stop(bkCluster);
    await bkApi.remove(bkCluster);
  });

  it('updateBroker', async () => {
    const bkCluster = await generateBroker();
    const newParams = {
      clientPort: 2222,
      jmxPort: 3333,
    };
    const newBk = { ...bkCluster, ...newParams };

    await bkApi.create(bkCluster);

    const result = await bkApi.update(newBk);

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
    expect(name).to.eq(bkCluster.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(bkCluster.group);

    expect(nodeNames).to.be.an('array');
    expect(nodeNames).have.lengthOf(1);

    expect(clientPort).to.be.a('number');
    expect(clientPort).to.eq(2222);

    expect(jmxPort).to.be.a('number');
    expect(jmxPort).to.eq(3333);

    expect(imageName).to.be.a('string');

    expect(tags.name).to.eq(bkCluster.name);
  });

  it('startBroker', async () => {
    const bkCluster = await generateBroker();

    await bkApi.create(bkCluster);
    const undefinedBkRes = await bkApi.get(bkCluster);
    expect(undefinedBkRes.state).to.be.undefined;

    const runningBkRes = await bkApi.start(bkCluster);
    expect(runningBkRes.state).to.eq('RUNNING');
  });

  it('stopBroker', async () => {
    const bkCluster = await generateBroker();

    await bkApi.create(bkCluster);
    const undefinedBkRes = await bkApi.get(bkCluster);
    expect(undefinedBkRes.state).to.be.undefined;

    const runningBkRes = await bkApi.start(bkCluster);
    expect(runningBkRes.state).to.eq('RUNNING');
    expect(runningBkRes.settings.nodeNames).have.lengthOf(1);

    const stopBkRes = await bkApi.stop(bkCluster);
    expect(stopBkRes.state).to.be.undefined;

    const result = await bkApi.remove(bkCluster);
    const brokers = result.map(bk => bk.settings.name);
    expect(brokers.includes(bkCluster.name)).to.be.false;
  });

  it('addNodeToBroker', async () => {
    const bkCluster = await generateBroker();

    await bkApi.create(bkCluster);
    const undefinedBkRes = await bkApi.get(bkCluster);
    expect(undefinedBkRes.state).to.be.undefined;

    const runningBkRes = await bkApi.start(bkCluster);
    expect(runningBkRes.state).to.eq('RUNNING');
    expect(runningBkRes.settings.nodeNames).have.lengthOf(1);

    const { node: newNode } = await createServices({ withNode: true });
    const newParams = Object.assign({}, bkCluster, {
      nodeName: newNode.hostname,
    });
    const result = await bkApi.addNode(newParams);
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
    expect(aliveNodes).have.lengthOf(2);

    expect(lastModified).to.be.a('number');

    expect(state).to.eq('RUNNING');

    expect(error).to.be.undefined;

    expect(name).to.be.a('string');
    expect(name).to.eq(bkCluster.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(bkCluster.group);

    expect(nodeNames).to.be.an('array');
    expect(nodeNames).have.lengthOf(2);
    expect(nodeNames.sort()).to.deep.eq(
      newParams.nodeNames.concat(newParams.nodeName).sort(),
    );

    expect(clientPort).to.be.a('number');
    expect(jmxPort).to.be.a('number');

    expect(imageName).to.be.a('string');

    expect(tags.name).to.eq(bkCluster.name);
  });

  it('removeNodeFromBroker', async () => {
    const bkCluster = await generateBroker();

    await bkApi.create(bkCluster);
    const undefinedBkRes = await bkApi.get(bkCluster);
    expect(undefinedBkRes.state).to.be.undefined;

    const runningBkRes = await bkApi.start(bkCluster);
    expect(runningBkRes.state).to.eq('RUNNING');
    expect(runningBkRes.settings.nodeNames).have.lengthOf(1);

    const { node: newNode } = await createServices({ withNode: true });
    const newParams = Object.assign({}, bkCluster, {
      nodeName: newNode.hostname,
    });
    const twoNodeBkData = await bkApi.addNode(newParams);
    expect(twoNodeBkData.aliveNodes).to.be.an('array');
    expect(twoNodeBkData.aliveNodes).have.lengthOf(2);
    expect(twoNodeBkData.settings.nodeNames).to.be.an('array');
    expect(twoNodeBkData.settings.nodeNames).have.lengthOf(2);

    const result = await bkApi.removeNode(newParams);
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
    expect(name).to.eq(bkCluster.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(bkCluster.group);

    expect(nodeNames).to.be.an('array');
    expect(nodeNames).have.lengthOf(1);
    expect(nodeNames).to.deep.eq(newParams.nodeNames);

    expect(clientPort).to.be.a('number');
    expect(jmxPort).to.be.a('number');

    expect(imageName).to.be.a('string');

    expect(tags.name).to.eq(bkCluster.name);
  });
});
