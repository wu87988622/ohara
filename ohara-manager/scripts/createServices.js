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

/* eslint-disable no-process-exit, no-console */

const api = require('../utils/apiHandler');

const zkName = 'zk' + api.randomName();
const bkName = 'bk' + api.randomName();

async function createServices(
  configurator,
  nodeHost,
  nodePort,
  nodeUser,
  nodePass,
) {
  await api.createNode(configurator, nodeHost, nodePort, nodeUser, nodePass);
  await api.waitCreate(configurator, 'nodes', nodeHost);
  await api.createZk(configurator, zkName, nodeHost);
  await api.waitCreate(configurator, 'zookeepers', zkName);
  await api.createBk(configurator, zkName, bkName, nodeHost);
  await api.waitCreate(configurator, 'brokers', bkName);
  await api.fileHelper(zkName, bkName);
}

module.exports = createServices;
