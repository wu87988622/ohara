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
const fs = require('fs');

async function createNode(baseUrl, nodeHost, nodePort, nodeUser, nodePass) {
  await axios.post(`${baseUrl}/nodes`, {
    name: nodeHost,
    port: nodePort,
    user: nodeUser,
    password: nodePass,
  });
}

async function createZk(baseUrl, zkName, nodeHost) {
  await axios.post(`${baseUrl}/zookeepers`, {
    clientPort: randomPort(),
    electionPort: randomPort(),
    peerPort: randomPort(),
    name: zkName,
    nodeNames: [nodeHost],
  });
}

async function createBk(baseUrl, zkName, bkName, nodeHost) {
  await axios.post(`${baseUrl}/brokers`, {
    clientPort: randomPort(),
    exporterPort: randomPort(),
    jmxPort: randomPort(),
    zookeeperClusterName: zkName,
    name: bkName,
    nodeNames: [nodeHost],
  });
}

async function getApi(baseUrl, api, name) {
  var res = await axios.get(`${baseUrl}/${api}`);
  if (res.data.length == 0) {
    return false;
  }
  var sevices = res.data.some(e => e.name == name);
  if (sevices) {
    return true;
  } else {
    return false;
  }
}

async function cleanNode(baseUrl, nodeHost) {
  await axios.delete(`${baseUrl}/nodes/${nodeHost}?force=true`);
}

async function cleanZk(baseUrl, zkname) {
  await axios.delete(`${baseUrl}/zookeepers/${zkname}?force=true`);
}

async function cleanBk(baseUrl, bkname) {
  await axios.delete(`${baseUrl}/brokers/${bkname}?force=true`);
}

async function cleanWk(baseUrl, wkname) {
  await axios.delete(`${baseUrl}/workers/${wkname}?force=true`);
}

async function waitDelete(baseUrl, api, name) {
  const res = await axios.get(`${baseUrl}/` + api);
  if (res.data.length > 0) {
    var result = res.data.some(e => e.name == name);
    if (!result) {
      return;
    }
    sleep(1000);
    await waitDelete(baseUrl, api, name);
  }

  return;
}

async function waitCreate(baseUrl, api, name) {
  const res = await axios.get(`${baseUrl}/` + api);
  var result = res.data.some(e => e.name == name);

  if (!result) {
    sleep(1000);
    await waitCreate(baseUrl, api, name);
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

function fileHelper(zkName, bkName) {
  fs.access('scripts/servicesApi/service.json', function(err) {
    if (err) {
      fs.mkdirSync('scripts/servicesApi');
    }
    const zkjson = {
      name: zkName,
      serviceType: 'zookeepers',
    };
    const bkjson = {
      name: bkName,
      serviceType: 'brokers',
    };
    const data = JSON.stringify([zkjson, bkjson]);
    fs.writeFile('scripts/servicesApi/service.json', data, error => {
      error;
    });
  });
}

async function jsonLoop(jsons, key, fn, baseUrl) {
  for (let json of jsons) {
    if (!(await getApi(baseUrl, key, json.name))) {
      continue;
    }
    if (json.serviceType == key) {
      await fn(baseUrl, json.name);
      await waitDelete(baseUrl, key, json.name);
    }
  }
}

function getEnv() {
  return JSON.parse(fs.readFileSync('./client/cypress.env.json'));
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
  cleanWk,
  cleanNode,
  fileHelper,
  jsonLoop,
  getEnv,
};
