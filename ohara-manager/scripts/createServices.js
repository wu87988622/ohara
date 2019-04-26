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
const axios = require('axios');
const fs = require('fs');

const {
  configurator,
  port,
  nodeHost,
  nodePort,
  nodeUser,
  nodePass,
} = yargs.argv;

const zkName = 'zk' + randomName();
const bkName = 'bk' + randomName();

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
  await createNode();
  await wait('nodes', nodeHost);
  await createZk();
  await wait('zookeepers', zkName);
  await createBk();
  await wait('brokers', bkName);
  await fs.writeFile(
    'scripts/env/service.json',
    `{"zk":"${zkName}","bk":"${bkName}"}`,
    error => {
      error;
    },
  );
};

createServices();

async function createNode() {
  await axios.post(`http://${configurator}:${port}/v0/nodes`, {
    name: nodeHost,
    port: nodePort,
    user: nodeUser,
    password: nodePass,
  });
}

async function createZk() {
  await axios.post(`http://${configurator}:${port}/v0/zookeepers`, {
    clientPort: randomPort(),
    electionPort: randomPort(),
    peerPort: randomPort(),
    name: zkName,
    nodeNames: [nodeHost],
  });
}

async function createBk() {
  await axios.post(`http://${configurator}:${port}/v0/brokers`, {
    clientPort: randomPort(),
    exporterPort: randomPort(),
    jmxPort: randomPort(),
    zookeeperClusterName: zkName,
    name: bkName,
    nodeNames: [nodeHost],
  });
}

async function wait(api, name) {
  const res = await axios.get(`http://${configurator}:${port}/v0/` + api);
  var result = res.data.some(e => e.name == name);

  if (!result) {
    sleep(1000);
    await wait(api, name);
  }

  return;
}

function randomPort() {
  var min = 5000;
  var max = 65535;
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomName() {
  var text = '';
  var possible = 'abcdefghijklmnopqrstuvwxyz0123456789';

  for (var i = 0; i < 5; i++)
    text += possible.charAt(Math.floor(Math.random() * possible.length));

  return text;
}

function sleep(milliseconds) {
  var start = new Date().getTime();
  for (var i = 0; i < 1e7; i++) {
    if (new Date().getTime() - start > milliseconds) {
      break;
    }
  }
}
