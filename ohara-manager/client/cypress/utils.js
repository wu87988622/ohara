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

import * as generate from '../src/utils/generate';
import * as nodeApi from '../src/api/nodeApi';
import * as fileApi from '../src/api/fileApi';
import * as zkApi from '../src/api/zookeeperApi';
import * as bkApi from '../src/api/brokerApi';
import * as wkApi from '../src/api/workerApi';
import * as connectorApi from '../src/api/connectorApi';
import * as topicApi from '../src/api/topicApi';
import * as streamApi from '../src/api/streamApi';
import * as objectApi from '../src/api/objectApi';

export const createServices = async ({
  withWorker = false,
  withBroker = false,
  withZookeeper = false,
  withNode = false,
} = {}) => {
  let result = {};

  if (withNode) {
    const node = {
      hostname: generate.serviceName({ prefix: 'node' }),
      port: generate.port(),
      user: generate.userName(),
      password: generate.password(),
    };
    const nodeRes = await nodeApi.create(node);
    if (!nodeRes.errors) result.node = nodeRes.data;

    if (withZookeeper) {
      const zookeeper = {
        name: generate.serviceName({ prefix: 'zk' }),
        group: generate.serviceName({ prefix: 'group' }),
        nodeNames: [node.hostname],
      };
      await zkApi.create(zookeeper);
      // zookeeper api will make sure the service is starting, we can skip the checking
      const zkRes = await zkApi.start(zookeeper);
      if (!zkRes.errors) result.zookeeper = zkRes.data.settings;

      if (withBroker) {
        const broker = {
          name: generate.serviceName({ prefix: 'bk' }),
          group: generate.serviceName({ prefix: 'group' }),
          nodeNames: [node.hostname],
          zookeeperClusterKey: {
            name: zookeeper.name,
            group: zookeeper.group,
          },
        };
        await bkApi.create(broker);
        // broker api will make sure the service is starting, we can skip the checking
        const bkRes = await bkApi.start(broker);
        if (!bkRes.errors) result.broker = bkRes.data.settings;

        if (withWorker) {
          const worker = {
            name: generate.serviceName({ prefix: 'wk' }),
            group: generate.serviceName({ prefix: 'group' }),
            nodeNames: [node.hostname],
            brokerClusterKey: {
              name: broker.name,
              group: broker.group,
            },
          };
          await wkApi.create(worker);
          // worker api will make sure the service is starting, we can skip the checking
          const wkRes = await wkApi.start(worker);
          if (!wkRes.errors) result.worker = wkRes.data.settings;
        }
      }
    }
  }

  return result;
};

export const deleteAllServices = async () => {
  // delete all connectors
  const connectRes = await connectorApi.getAll();
  if (connectRes.errors) {
    throw new Error(JSON.stringify(connectRes));
  }
  const connects = connectRes.data;
  // we don't care the execute order of each individual connect was done or not.
  // Using Promise.all() to make sure all connects were stopped & deleted.
  await Promise.all(
    connects.map(connect => connectorApi.stop(connect.settings)),
  );
  await Promise.all(
    connects.map(connect => connectorApi.remove(connect.settings)),
  );

  // delete all workers
  const workerRes = await wkApi.getAll();
  if (workerRes.errors) {
    throw new Error(JSON.stringify(workerRes));
  }
  const workers = workerRes.data;
  // we don't care the execute order of each individual worker was done or not.
  // Using Promise.all() to make sure all workers were stopped & deleted.
  await Promise.all(workers.map(wk => wkApi.stop(wk.settings)));
  await Promise.all(workers.map(wk => wkApi.remove(wk.settings)));

  // delete all streams
  const streamRes = await streamApi.getAll();
  if (streamRes.errors) {
    throw new Error(JSON.stringify(streamRes));
  }
  const streams = streamRes.data;
  // we don't care the execute order of each individual stream was done or not.
  // Using Promise.all() to make sure all streams were stopped & deleted.
  await Promise.all(streams.map(stream => streamApi.stop(stream.settings)));
  await Promise.all(streams.map(stream => streamApi.remove(stream.settings)));

  // delete all topics
  const topicRes = await topicApi.getAll();
  if (topicRes.errors) {
    throw new Error(JSON.stringify(topicRes));
  }
  const topics = topicRes.data;
  // we don't care the execute order of each individual topic was done or not.
  // Using Promise.all() to make sure all topics were stopped & deleted.
  await Promise.all(topics.map(topic => topicApi.stop(topic.settings)));
  await Promise.all(topics.map(topic => topicApi.remove(topic.settings)));

  // delete all brokers
  const brokerRes = await bkApi.getAll();
  if (brokerRes.errors) {
    throw new Error(JSON.stringify(brokerRes));
  }
  const brokers = brokerRes.data;
  // we don't care the execute order of each individual broker was done or not.
  // Using Promise.all() to make sure all brokers were stopped & deleted.
  await Promise.all(brokers.map(bk => bkApi.stop(bk.settings)));
  await Promise.all(brokers.map(bk => bkApi.remove(bk.settings)));

  // delete all zookeepers
  const zookeeperRes = await zkApi.getAll();
  if (zookeeperRes.errors) {
    throw new Error(JSON.stringify(zookeeperRes));
  }
  const zookeepers = zookeeperRes.data;
  // we don't care the execute order of each individual zookeeper was done or not.
  // Using Promise.all() to make sure all zookeepers were stopped & deleted.
  await Promise.all(zookeepers.map(zk => zkApi.stop(zk.settings)));
  await Promise.all(zookeepers.map(zk => zkApi.remove(zk.settings)));

  // delete all nodes
  const nodeRes = await nodeApi.getAll();
  if (nodeRes.errors) {
    throw new Error(JSON.stringify(nodeRes));
  }
  const nodes = nodeRes.data;
  // we don't care the execute order of each individual node was done or not.
  // Using Promise.all() to make sure all nodes were deleted.
  await Promise.all(nodes.map(node => nodeApi.remove(node)));

  // delete all files
  const fileRes = await fileApi.getAll();
  if (fileRes.errors) {
    throw new Error(JSON.stringify(fileRes));
  }
  const files = fileRes.data;
  // we don't care the execute order of each individual file was done or not.
  // Using Promise.all() to make sure all files were deleted.
  await Promise.all(files.map(file => fileApi.remove(file)));

  // delete all objects
  const objectRes = await objectApi.getAll();
  if (objectRes.errors) {
    throw new Error(JSON.stringify(objectRes));
  }
  const objects = objectRes.data;
  // we don't care the execute order of each individual object was done or not.
  // Using Promise.all() to make sure all objects were deleted.
  await Promise.all(objects.map(object => objectApi.remove(object)));
};
