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

async function createNode(
  configurator,
  nodeHost,
  nodePort,
  nodeUser,
  nodePass,
) {
  await axios.post(`${configurator}/nodes`, {
    name: nodeHost,
    port: nodePort,
    user: nodeUser,
    password: nodePass,
  });
}

async function createZk(configurator, zkName, nodeHost) {
  await axios.post(`${configurator}/zookeepers`, {
    clientPort: randomPort(),
    electionPort: randomPort(),
    peerPort: randomPort(),
    name: zkName,
    nodeNames: [nodeHost],
  });
}

async function createBk(configurator, zkName, bkName, nodeHost) {
  await axios.post(`${configurator}/brokers`, {
    clientPort: randomPort(),
    exporterPort: randomPort(),
    jmxPort: randomPort(),
    zookeeperClusterName: zkName,
    name: bkName,
    nodeNames: [nodeHost],
  });
}

async function getApi(configurator, api, name) {
  var res = await axios.get(`${configurator}/${api}`);
  var result = res.data.some(e => e.name == name);
  if (res.data.length == 0) {
    return false;
  }
  if (result) {
    return true;
  } else {
    return false;
  }
}

async function cleanNode(configurator, nodeHost) {
  await axios.delete(`${configurator}/nodes/${nodeHost}?force=true`);
}

async function cleanZk(configurator, zkname) {
  await axios.delete(`${configurator}/zookeepers/${zkname}?force=true`);
}

async function cleanBk(configurator, bkname) {
  await axios.delete(`${configurator}/brokers/${bkname}?force=true`);
}

async function cleanWk(configurator, wkname) {
  await axios.delete(`${configurator}/workers/${wkname}?force=true`);
}

async function waitDelete(configurator, api, name) {
  const res = await axios.get(`${configurator}/` + api);
  if (res.data.length > 0) {
    var result = res.data.some(e => e.name == name);
    if (!result) {
      return;
    }
    sleep(1000);
    await waitDelete(configurator, api, name);
  }

  return;
}

async function waitCreate(configurator, api, name) {
  const res = await axios.get(`${configurator}/` + api);
  var result = res.data.some(e => e.name == name);

  if (!result) {
    sleep(1000);
    await waitCreate(configurator, api, name);
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
    let zkjson = {
      name: zkName,
      serviceType: 'zookeepers',
    };
    let bkjson = {
      name: bkName,
      serviceType: 'brokers',
    };
    let data = JSON.stringify([zkjson, bkjson]);
    fs.writeFile('scripts/servicesApi/service.json', data, error => {
      error;
    });
  });
}

async function jsonLoop(jsons, key, fun, configurator) {
  for (let json of jsons) {
    if (!(await getApi(configurator, key, json.name))) {
      continue;
    }
    if (json.serviceType == key) {
      await fun(configurator, json.name);
      await waitDelete(configurator, key, json.name);
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
  cleanWk,
  cleanNode,
  fileHelper,
  jsonLoop,
};
