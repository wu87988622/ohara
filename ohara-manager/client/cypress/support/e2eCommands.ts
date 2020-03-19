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

/* eslint-disable no-unused-vars */
/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable no-console */

import '@testing-library/cypress/add-commands';

import { KIND } from '../../src/const';

import * as waitUtil from '../../src/api/utils/waitUtils';
import * as nodeApi from '../../src/api/nodeApi';
import * as zkApi from '../../src/api/zookeeperApi';
import * as bkApi from '../../src/api/brokerApi';
import * as wkApi from '../../src/api/workerApi';
import * as connectorApi from '../../src/api/connectorApi';
import * as topicApi from '../../src/api/topicApi';
import * as objectApi from '../../src/api/objectApi';
import * as generate from '../../src/utils/generate';
import { sleep } from '../../src/utils/common';
import { hashByGroupAndName } from '../../src/utils/sha';
import { NodeResponse } from '../../src/api/apiInterface/nodeInterface';
import { ClusterResponse } from '../../src/api/apiInterface/clusterInterface';
import { waitFor } from '../utils';
import { RESOURCE } from '../../src/api/utils/apiUtils';
import { TopicResponse } from '../../src/api/apiInterface/topicInterface';
import { SOURCES } from '../../src/api/apiInterface/connectorInterface';

declare global {
  namespace Cypress {
    type ServiceData = {
      workspaceName?: string;
      node?: NodeResponse;
      zookeeper?: ClusterResponse;
      broker?: ClusterResponse;
      worker?: ClusterResponse;
      topic?: TopicResponse;
    };

    interface Chainable {
      createServices: ({
        withWorkspace,
        withTopic,
      }: {
        withWorkspace?: boolean;
        withTopic?: boolean;
      }) => Promise<Cypress.ServiceData>;
      produceTopicData: (
        workspaceName?: string,
        topic?: TopicResponse,
      ) => Chainable<void>;
    }
  }
}

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
  async ({
    withWorkspace = false,
    withTopic = false,
  }: { withWorkspace?: boolean; withTopic?: boolean } = {}) => {
    let result: Cypress.ServiceData = {};

    const workspaceName = generate.serviceName({ prefix });
    result.workspaceName = workspaceName;

    const node = {
      hostname: nodeHost,
      port: nodePort,
      user: nodeUser,
      password: nodePass,
    };
    // In K8S mode, the node may be created by configurator
    // We need to make sure it's missing before creation
    const nodeRes = await nodeApi
      .get(node.hostname)
      .then(res => res)
      .catch(() => {
        // node is not present, we are ready to create
        return nodeApi.create(node);
      });
    result.node = nodeRes;

    if (withWorkspace) {
      const zookeeper = {
        name: workspaceName,
        group: zookeeperGroup,
        nodeNames: [node.hostname],
      };
      await zkApi.create(zookeeper);
      await zkApi.start(zookeeper);
      const zkRes = await waitFor(
        RESOURCE.ZOOKEEPER,
        zookeeper,
        waitUtil.waitForRunning,
      );
      result.zookeeper = zkRes;

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
      await bkApi.start(broker);
      const bkRes = await waitFor(
        RESOURCE.BROKER,
        broker,
        waitUtil.waitForRunning,
      );
      result.broker = bkRes;

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
      await wkApi.start(worker);
      const wkRes = await waitFor(
        RESOURCE.WORKER,
        worker,
        waitUtil.waitForRunning,
      );
      result.worker = wkRes;

      // wait until the connector list show up
      await waitFor(
        `${RESOURCE.INSPECT}/${KIND.worker}`,
        worker,
        waitUtil.waitForClassInfosReady,
      );

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
        await topicApi.start(topic);
        const topicRes = await waitFor(
          RESOURCE.TOPIC,
          topic,
          waitUtil.waitForTopicReady,
        );
        result.topic = topicRes;
      }
    }

    return result;
  },
);

// Note: this custom command is a "heavy" command, may take almost 40 seconds to accomplish
// make sure you have set enough timeout of defaultCommandTimeout in cypress.e2e.json
Cypress.Commands.add(
  'produceTopicData',
  async (workerName?: string, topic?: TopicResponse) => {
    if (!workerName || !topic) {
      console.error('the workspaceName and topic are required fields');
      return;
    }
    const connector = {
      name: generate.serviceName({ prefix: 'perf' }),
      // it is ok to use random group here; we just need a temp connector to produce data
      group: generate.serviceName({ prefix: 'group' }),
      connector__class: SOURCES.perf,
      topicKeys: [
        {
          name: topic.data.name,
          group: topic.data.group,
        },
      ],
      workerClusterKey: {
        name: workerName,
        group: workerGroup,
      },
    };

    await connectorApi.create(connector);
    await connectorApi.start(connector);

    // sleep 10 seconds for perf connector to write some data to topic
    await sleep(10000);

    // remove this temp connector
    await connectorApi.forceStop(connector);
    await connectorApi.remove(connector);
  },
);

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
