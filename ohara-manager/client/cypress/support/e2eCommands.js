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

import '@testing-library/cypress/add-commands';

import { KIND } from '../../src/const';
import wait from '../../src/api/waitApi';

import * as waitUtil from '../../src/api/utils/waitUtils';
import * as nodeApi from '../../src/api/nodeApi';
import * as zkApi from '../../src/api/zookeeperApi';
import * as bkApi from '../../src/api/brokerApi';
import * as wkApi from '../../src/api/workerApi';
import * as connectorApi from '../../src/api/connectorApi';
import * as topicApi from '../../src/api/topicApi';
import * as objectApi from '../../src/api/objectApi';
import * as URL from '../../src/api/utils/url';
import * as generate from '../../src/utils/generate';
import { sleep } from '../../src/utils/common';
import { hashByGroupAndName } from '../../src/utils/sha';

// Get this environment for further usage
const prefix = Cypress.env('servicePrefix');
const nodeHost = Cypress.env('nodeHost');
const nodePort = Cypress.env('nodePort');
const nodeUser = Cypress.env('nodeUser');
const nodePass = Cypress.env('nodePass');

const workspaceGroup = 'workspace';
const zookeeperGroup = 'zookeeper';
const brokerGroup = 'broker';
const workerGroup = 'worker';

const SINGLE_COMMAND_TIMEOUT = 5000;

Cypress.Commands.add(
  'createServices',
  async ({ withWorkspace = false, withTopic = false } = {}) => {
    let result = {};

    const workspaceName = generate.serviceName({ prefix });
    result.workspaceName = workspaceName;

    const node = {
      hostname: nodeHost,
      port: nodePort,
      user: nodeUser,
      password: nodePass,
    };
    const nodeRes = await nodeApi.create(node);
    if (!nodeRes.errors) result.node = nodeRes.data;

    if (withWorkspace) {
      const zookeeper = {
        name: workspaceName,
        group: zookeeperGroup,
        nodeNames: [node.hostname],
      };
      await zkApi.create(zookeeper);
      await objectApi.create(zookeeper);
      // zookeeper api will make sure the service is starting, we can skip the checking
      const zkRes = await zkApi.start(zookeeper);
      if (!zkRes.errors) result.zookeeper = zkRes.data;

      const broker = {
        name: workspaceName,
        group: brokerGroup,
        nodeNames: [node.hostname],
        zookeeperClusterKey: {
          name: zookeeper.name,
          group: zookeeper.group,
        },
      };
      await bkApi.create(broker);
      await objectApi.create(broker);
      // broker api will make sure the service is starting, we can skip the checking
      const bkRes = await bkApi.start(broker);
      if (!bkRes.errors) result.broker = bkRes.data;

      const worker = {
        name: workspaceName,
        group: workerGroup,
        nodeNames: [node.hostname],
        brokerClusterKey: {
          name: broker.name,
          group: broker.group,
        },
      };
      await wkApi.create(worker);
      await objectApi.create(worker);
      // worker api will make sure the service is starting, we can skip the checking
      const wkRes = await wkApi.start(worker);
      if (!wkRes.errors) result.worker = wkRes.data;

      // wait until the connector list show up
      const waitRes = await wait({
        url: `${URL.INSPECT_URL}/${KIND.worker}/${workspaceName}?group=${workerGroup}`,
        checkFn: waitUtil.waitForConnectReady,
        // we don't need to inspect too frequently
        sleep: 5000,
      });
      if (waitRes.errors) {
        throw new Error(
          `Get connector list of worker ${workspaceName} failed.`,
        );
      }

      const workspace = {
        name: workspaceName,
        group: workspaceGroup,
        tags: {
          name: workspaceName,
          group: workspaceGroup,
          nodeNames: [node.hostname],
        },
      };
      objectApi.create(workspace);

      if (withTopic) {
        const topic = {
          name: generate.serviceName({ prefix: 'topic' }),
          group: hashByGroupAndName(workspaceGroup, workspaceName),
          nodeNames: [node.hostname],
          brokerClusterKey: {
            name: broker.name,
            group: broker.group,
          },
          tags: {
            isShared: true,
            parentKey: {
              name: workspaceName,
              group: workspaceGroup,
            },
          },
        };

        await topicApi.create(topic);
        // topic api will make sure the service is starting, we can skip the checking
        const topicRes = await topicApi.start(topic);
        if (!topicRes.errors) result.topic = topicRes.data;
      }
    }

    return result;
  },
);

// Note: this custom command is a "heavy" command, may take almost 40 seconds to accomplish
// make sure you have set enough timeout of defaultCommandTimeout in cypress.e2e.json
Cypress.Commands.add('produceTopicData', async (workerName, topic) => {
  const connector = {
    name: generate.serviceName({ prefix: 'perf' }),
    // it is ok to use random group here; we just need a temp connector to produce data
    group: generate.serviceName({ prefix: 'group' }),
    connector__class: connectorApi.connectorSources.perf,
    columns: ['name', 'value'],
    topicKeys: [
      {
        name: topic.name,
        group: topic.group,
      },
    ],
    workerClusterKey: {
      name: workerName,
      group: workerGroup,
    },
  };

  const connectorRes = await connectorApi.create(connector);
  if (connectorRes.errors) throw new Error(connectorRes.title);

  await connectorApi.start(connector);

  // sleep 10 seconds for perf connector to write some data to topic
  await sleep(10000);

  // remove this temp connector
  await connectorApi.forceStop(connector);
  await connectorApi.remove(connector);

  return connector;
});

Cypress.Commands.overwrite('get', (originFn, subject, options) => {
  // we only wait a few seconds for simple command instead of using defaultCommandTimeout
  const customOptions = {
    ...options,
    timeout: SINGLE_COMMAND_TIMEOUT,
  };
  return originFn(subject, customOptions);
});

Cypress.Commands.overwrite('click', (originFn, subject, options) => {
  // we only wait a few seconds for simple command instead of using defaultCommandTimeout
  const customOptions = {
    ...options,
    timeout: SINGLE_COMMAND_TIMEOUT,
  };
  return originFn(subject, customOptions);
});
