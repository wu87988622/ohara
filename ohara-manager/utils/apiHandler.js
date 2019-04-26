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

const axios = require('axios');

async function createNode(
  configurator,
  port,
  nodeHost,
  nodePort,
  nodeUser,
  nodePass,
) {
  await axios.post(`http://${configurator}:${port}/v0/nodes`, {
    name: nodeHost,
    port: nodePort,
    user: nodeUser,
    password: nodePass,
  });
}

async function createZk(configurator, port, zkName, nodeHost) {
  await axios.post(`http://${configurator}:${port}/v0/zookeepers`, {
    clientPort: randomPort(),
    electionPort: randomPort(),
    peerPort: randomPort(),
    name: zkName,
    nodeNames: [nodeHost],
  });
}

async function createBk(configurator, port, zkName, bkName, nodeHost) {
  await axios.post(`http://${configurator}:${port}/v0/brokers`, {
    clientPort: randomPort(),
    exporterPort: randomPort(),
    jmxPort: randomPort(),
    zookeeperClusterName: zkName,
    name: bkName,
    nodeNames: [nodeHost],
  });
}

async function cleanNode(configurator, port, nodeHost) {
  await axios.delete(`http://${configurator}:${port}/v0/nodes/${nodeHost}`);
}

async function cleanZk(configurator, port, zkname) {
  await axios.delete(`http://${configurator}:${port}/v0/zookeepers/${zkname}`);
}

async function cleanBk(configurator, port, bkname) {
  await axios.delete(`http://${configurator}:${port}/v0/brokers/${bkname}`);
}

async function waitDelete(configurator, port, api) {
  const res = await axios.get(`http://${configurator}:${port}/v0/` + api);
  if (res.data.length > 0) {
    sleep(1000);
    await waitDelete(configurator, port, api);
  }

  return;
}

async function waitCreate(configurator, port, api, name) {
  const res = await axios.get(`http://${configurator}:${port}/v0/` + api);
  var result = res.data.some(e => e.name == name);

  if (!result) {
    sleep(1000);
    await waitCreate(configurator, port, api, name);
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

module.exports = {
  createNode,
  createZk,
  createBk,
  waitCreate,
  waitDelete,
  randomName,
  cleanBk,
  cleanZk,
  cleanNode,
};
