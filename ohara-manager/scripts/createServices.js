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

const yargs = require('yargs');
const api = require('../utils/apiHandler');
const fs = require('fs');

const {
  configurator,
  port,
  nodeHost,
  nodePort,
  nodeUser,
  nodePass,
} = yargs.argv;

const zkName = 'zk' + api.randomName();
const bkName = 'bk' + api.randomName();

debug('configurator: ', configurator);
debug('port: ', port);
debug('nodeHost: ', nodeHost || 'Not input.');
debug('nodePort: ', nodePort || 'Not input.');
debug('nodeUser: ', nodeUser || 'Not input.');
debug('nodePass: ', nodePass || 'Not input.');

function debug(...message) {
  console.log(...message);
}

const createServices = async () => {
  await api.createNode(
    configurator,
    port,
    nodeHost,
    nodePort,
    nodeUser,
    nodePass,
  );
  await api.waitCreate(configurator, port, 'nodes', nodeHost);
  await api.createZk(configurator, port, zkName, nodeHost);
  await api.waitCreate(configurator, port, 'zookeepers', zkName);
  await api.createBk(configurator, port, zkName, bkName, nodeHost);
  await api.waitCreate(configurator, port, 'brokers', bkName);
  await fs.writeFile(
    'scripts/env/service.json',
    `{"zk":"${zkName}","bk":"${bkName}"}`,
    error => {
      error;
    },
  );
};

createServices();
