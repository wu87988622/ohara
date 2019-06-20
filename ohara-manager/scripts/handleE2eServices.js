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

const fs = require('fs');
const axios = require('axios');
const commonUtils = require('../utils/commonUtils');

/* eslint-disable no-console */
const randomName = () => {
  const possible = 'abcdefghijklmnopqrstuvwxyz0123456789';
  let text = '';

  for (let i = 0; i < 5; i++) {
    text += possible.charAt(Math.floor(Math.random() * possible.length));
  }

  return text;
};

const createNode = async (baseUrl, nodeHost, nodePort, nodeUser, nodePass) => {
  await axios.post(`${baseUrl}/nodes`, {
    name: nodeHost,
    port: nodePort,
    user: nodeUser,
    password: nodePass,
  });
};

const createZk = async (baseUrl, zkName, nodeHost) => {
  await axios.post(`${baseUrl}/zookeepers`, {
    clientPort: commonUtils.randomPort(),
    electionPort: commonUtils.randomPort(),
    peerPort: commonUtils.randomPort(),
    name: zkName,
    nodeNames: [nodeHost],
  });
};

const createBk = async (baseUrl, zkName, bkName, nodeHost) => {
  await axios.post(`${baseUrl}/brokers`, {
    clientPort: commonUtils.randomPort(),
    exporterPort: commonUtils.randomPort(),
    jmxPort: commonUtils.randomPort(),
    zookeeperClusterName: zkName,
    name: bkName,
    nodeNames: [nodeHost],
  });
};

const cleanZk = async (baseUrl, zkname) => {
  await axios.delete(`${baseUrl}/zookeepers/${zkname}?force=true`);
};

const cleanBk = async (baseUrl, bkname) => {
  await axios.delete(`${baseUrl}/brokers/${bkname}?force=true`);
};

const cleanWk = async (baseUrl, wkname) => {
  await axios.delete(`${baseUrl}/workers/${wkname}?force=true`);
};

const getApi = async (baseUrl, api, name) => {
  const res = await axios.get(`${baseUrl}/${api}`);
  if (res.data.length === 0) {
    return false;
  }

  const services = res.data.some(e => e.name == name);

  if (services) {
    return true;
  } else {
    return false;
  }
};

const waitDelete = async (baseUrl, api, name) => {
  const res = await axios.get(`${baseUrl}/${api}`);
  if (res.data.length > 0) {
    const result = res.data.some(e => e.name == name);

    if (!result) return;

    await commonUtils.sleep(1000);
    await waitDelete(baseUrl, api, name);
  }

  return;
};

const waitCreate = async (baseUrl, api, name) => {
  const res = await axios.get(`${baseUrl}/${api}`);
  const result = res.data.some(e => e.name == name);

  if (!result) {
    await commonUtils.sleep(1000);
    await waitCreate(baseUrl, api, name);
  }

  return;
};

const waitContainersCreate = async (baseUrl, name) => {
  const res = await axios.get(`${baseUrl}/containers/${name}`);
  if (
    typeof res === 'undefined' ||
    res.data[0].containers.length == 0 ||
    res.data[0].containers[0].state !== 'RUNNING'
  ) {
    await commonUtils.sleep(1000);
    await waitContainersCreate(baseUrl, name);
  }
  return;
};

const fileHelper = (zkName, bkName) => {
  const filePath = 'scripts/servicesApi/service.json';

  fs.access(filePath, function(err) {
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
    fs.writeFile(filePath, data, error => {
      error;
    });
  });
};

const jsonLoop = async (jsons, key, fn, baseUrl) => {
  for (let json of jsons) {
    if (!(await getApi(baseUrl, key, json.name))) {
      continue;
    }
    if (json.serviceType == key) {
      await fn(baseUrl, json.name);
      await waitDelete(baseUrl, key, json.name);
    }
  }
};

const getDefaultEnv = () => {
  const filePath = './client/cypress.env.json';
  if (fs.existsSync(filePath)) {
    return JSON.parse(fs.readFileSync(filePath));
  }

  return {};
};

async function createServices({
  configurator,
  nodeHost,
  nodePort,
  nodeUser,
  nodePass,
}) {
  const zkName = 'zk' + randomName();
  const bkName = 'bk' + randomName();

  await createNode(configurator, nodeHost, nodePort, nodeUser, nodePass);
  await waitCreate(configurator, 'nodes', nodeHost);
  await createZk(configurator, zkName, nodeHost);
  await waitContainersCreate(configurator, zkName);
  await createBk(configurator, zkName, bkName, nodeHost);
  await waitContainersCreate(configurator, bkName);
  await fileHelper(zkName, bkName);
}

async function cleanServices(configurator) {
  const file = fs.readFileSync('scripts/servicesApi/service.json');
  const jsons = JSON.parse(file);

  await jsonLoop(jsons, 'workers', cleanWk, configurator);
  await jsonLoop(jsons, 'brokers', cleanBk, configurator);
  await jsonLoop(jsons, 'zookeepers', cleanZk, configurator);
}

module.exports = {
  cleanServices,
  createServices,
  getDefaultEnv,
};
