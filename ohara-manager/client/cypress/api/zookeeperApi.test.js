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
import * as zkApi from '../../src/api/zookeeperApi';
import { createServices, deleteAllServices } from '../utils';

const generateZookeeper = async () => {
  const { node } = await createServices({ withNode: true });
  const zkName = generate.serviceName({ prefix: 'zk' });
  const zookeeper = {
    name: zkName,
    group: generate.serviceName({ prefix: 'group' }),
    nodeNames: [node.hostname],
    tags: {
      name: zkName,
    },
  };
  return zookeeper;
};

describe('Zookeeper API', () => {
  beforeEach(() => deleteAllServices());

  it('createZookeeper', async () => {
    const zkCluster = await generateZookeeper();
    const result = await zkApi.create(zkCluster);
    expect(result.errors).to.be.undefined;

    const { aliveNodes, lastModified, state, error } = result.data;
    const {
      name,
      group,
      nodeNames,
      clientPort,
      electionPort,
      peerPort,
      imageName,
      tags,
    } = result.data;

    expect(aliveNodes).to.be.an('array');
    expect(aliveNodes).to.be.empty;

    expect(lastModified).to.be.a('number');

    expect(state).to.be.undefined;

    expect(error).to.be.undefined;

    expect(name).to.be.a('string');
    expect(name).to.eq(zkCluster.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(zkCluster.group);

    expect(nodeNames).to.be.an('array');
    expect(nodeNames).have.lengthOf(1);

    expect(clientPort).to.be.a('number');
    expect(electionPort).to.be.a('number');
    expect(peerPort).to.be.a('number');

    expect(imageName).to.be.a('string');

    expect(tags.name).to.eq(zkCluster.name);
  });

  it('fetchZookeeper', async () => {
    const zkCluster = await generateZookeeper();
    await zkApi.create(zkCluster);

    const result = await zkApi.get(zkCluster);
    expect(result.errors).to.be.undefined;

    const { aliveNodes, lastModified, state, error } = result.data;
    const {
      name,
      group,
      nodeNames,
      clientPort,
      electionPort,
      peerPort,
      imageName,
      tags,
    } = result.data;

    expect(aliveNodes).to.be.an('array');
    expect(aliveNodes).to.be.empty;

    expect(lastModified).to.be.a('number');

    expect(state).to.be.undefined;

    expect(error).to.be.undefined;

    expect(name).to.be.a('string');
    expect(name).to.eq(zkCluster.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(zkCluster.group);

    expect(nodeNames).to.be.an('array');
    expect(nodeNames).have.lengthOf(1);

    expect(clientPort).to.be.a('number');
    expect(electionPort).to.be.a('number');
    expect(peerPort).to.be.a('number');

    expect(imageName).to.be.a('string');

    expect(tags.name).to.eq(zkCluster.name);
  });

  it('fetchZookeepers', async () => {
    const zkClusterOne = await generateZookeeper();
    const zkClusterTwo = await generateZookeeper();

    await zkApi.create(zkClusterOne);
    await zkApi.create(zkClusterTwo);

    const result = await zkApi.getAll();
    expect(result.errors).to.be.undefined;

    const zookeepers = result.data.map(zk => zk.name);
    expect(zookeepers.includes(zkClusterOne.name)).to.be.true;
    expect(zookeepers.includes(zkClusterTwo.name)).to.be.true;
  });

  it('deleteZookeeper', async () => {
    const zkCluster = await generateZookeeper();

    // delete a non-running zookeeper
    await zkApi.create(zkCluster);
    const result = await zkApi.remove(zkCluster);
    expect(result.errors).to.be.undefined;

    const zookeepers = result.data.map(zk => zk.name);
    expect(zookeepers.includes(zkCluster.name)).to.be.false;

    // delete a running zookeeper
    await zkApi.create(zkCluster);
    const runningRes = await zkApi.start(zkCluster);
    expect(runningRes.errors).to.be.undefined;

    expect(runningRes.data.state).to.eq('RUNNING');

    await zkApi.stop(zkCluster);
    await zkApi.remove(zkCluster);
  });

  it('updateZookeeper', async () => {
    const zkCluster = await generateZookeeper();
    const newParams = {
      clientPort: 2222,
      electionPort: 3333,
      peerPort: 4444,
    };
    const newZk = { ...zkCluster, ...newParams };

    await zkApi.create(zkCluster);

    const result = await zkApi.update(newZk);
    expect(result.errors).to.be.undefined;

    const { aliveNodes, lastModified, state, error } = result.data;
    const {
      name,
      group,
      nodeNames,
      clientPort,
      electionPort,
      peerPort,
      imageName,
      tags,
    } = result.data;

    expect(aliveNodes).to.be.an('array');
    expect(aliveNodes).to.be.empty;

    expect(lastModified).to.be.a('number');

    expect(state).to.be.undefined;

    expect(error).to.be.undefined;

    expect(name).to.be.a('string');
    expect(name).to.eq(zkCluster.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(zkCluster.group);

    expect(nodeNames).to.be.an('array');
    expect(nodeNames).have.lengthOf(1);

    expect(clientPort).to.be.a('number');
    expect(clientPort).to.eq(2222);

    expect(electionPort).to.be.a('number');
    expect(electionPort).to.eq(3333);

    expect(peerPort).to.be.a('number');
    expect(peerPort).to.eq(4444);

    expect(imageName).to.be.a('string');

    expect(tags.name).to.eq(zkCluster.name);
  });

  it('startZookeeper', async () => {
    const zkCluster = await generateZookeeper();

    await zkApi.create(zkCluster);
    const undefinedZkRes = await zkApi.get(zkCluster);
    expect(undefinedZkRes.errors).to.be.undefined;
    expect(undefinedZkRes.data.state).to.be.undefined;

    const runningZkRes = await zkApi.start(zkCluster);
    expect(runningZkRes.errors).to.be.undefined;
    expect(runningZkRes.data.state).to.eq('RUNNING');
  });

  it('stopZookeeper', async () => {
    const zkCluster = await generateZookeeper();

    await zkApi.create(zkCluster);
    const undefinedZkRes = await zkApi.get(zkCluster);
    expect(undefinedZkRes.errors).to.be.undefined;
    expect(undefinedZkRes.data.state).to.be.undefined;

    const runningZkRes = await zkApi.start(zkCluster);
    expect(runningZkRes.errors).to.be.undefined;
    expect(runningZkRes.data.state).to.eq('RUNNING');
    expect(runningZkRes.data.nodeNames).have.lengthOf(1);

    const stopZkRes = await zkApi.stop(zkCluster);
    expect(stopZkRes.errors).to.be.undefined;
    expect(stopZkRes.data.state).to.be.undefined;

    const result = await zkApi.remove(zkCluster);
    expect(result.errors).to.be.undefined;
    const zookeepers = result.data.map(zk => zk.name);
    expect(zookeepers.includes(zkCluster.name)).to.be.false;
  });
});
