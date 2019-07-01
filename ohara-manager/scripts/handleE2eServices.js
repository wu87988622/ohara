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
const chalk = require('chalk');
const lodash = require('lodash');

const commonUtils = require('../utils/commonUtils');

/* eslint-disable no-console */
const randomServiceName = () => {
  const possible = 'abcdefghijklmnopqrstuvwxyz0123456789';
  let name = '';

  for (let i = 0; i < 5; i++) {
    name += possible.charAt(Math.floor(Math.random() * possible.length));
  }

  return name;
};

const createNode = (baseUrl, nodeHost, nodePort, nodeUser, nodePass) => {
  return axios.post(`${baseUrl}/nodes`, {
    name: nodeHost,
    port: nodePort,
    user: nodeUser,
    password: nodePass,
  });
};

const createZk = (baseUrl, zkName, nodeHost) => {
  return axios.post(`${baseUrl}/zookeepers`, {
    clientPort: commonUtils.randomPort(),
    electionPort: commonUtils.randomPort(),
    peerPort: commonUtils.randomPort(),
    name: zkName,
    nodeNames: [nodeHost],
  });
};

const createBk = (baseUrl, zkName, bkName, nodeHost) => {
  return axios.post(`${baseUrl}/brokers`, {
    clientPort: commonUtils.randomPort(),
    exporterPort: commonUtils.randomPort(),
    jmxPort: commonUtils.randomPort(),
    zookeeperClusterName: zkName,
    name: bkName,
    nodeNames: [nodeHost],
  });
};

const cleanZk = (baseUrl, zkName) => {
  return axios.delete(`${baseUrl}/zookeepers/${zkName}?force=true`);
};

const cleanBk = (baseUrl, bkName) => {
  return axios.delete(`${baseUrl}/brokers/${bkName}?force=true`);
};

const cleanWk = (baseUrl, wkName) => {
  return axios.delete(`${baseUrl}/workers/${wkName}?force=true`);
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

const waitForDeleteService = async (baseUrl, api, name) => {
  const res = await axios.get(`${baseUrl}/${api}`);
  if (res.data.length > 0) {
    const result = res.data.some(e => e.name == name);

    if (!result) return;

    await commonUtils.sleep(1000);
    await waitForDeleteService(baseUrl, api, name);
  }

  return;
};

const waitForCreateService = async (baseUrl, api, name) => {
  const res = await axios.get(`${baseUrl}/${api}`);
  const result = res.data.some(e => e.name == name);

  if (!result) {
    await commonUtils.sleep(1000);
    await waitForCreateService(baseUrl, api, name);
  }

  return;
};

const waitContainersCreate = async (baseUrl, name) => {
  const res = await axios.get(`${baseUrl}/containers/${name}`);
  const containers = lodash.get(res, 'data[0].containers[0].state', null);
  if (containers === null) {
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

    const zk = {
      name: zkName,
      serviceType: 'zookeepers',
    };

    const bk = {
      name: bkName,
      serviceType: 'brokers',
    };

    const data = JSON.stringify([zk, bk]);
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
      await waitForDeleteService(baseUrl, key, json.name);
    }
  }
};

exports.getDefaultEnv = () => {
  const filePath = './client/cypress.env.json';
  if (fs.existsSync(filePath)) {
    return JSON.parse(fs.readFileSync(filePath));
  }

  return {};
};

exports.createServices = async ({
  configurator,
  nodeHost,
  nodePort,
  nodeUser,
  nodePass,
}) => {
  console.log(chalk.blue('Creating services for this test run'));

  const zkName = 'zk' + randomServiceName();
  const bkName = 'bk' + randomServiceName();

  try {
    await createNode(configurator, nodeHost, nodePort, nodeUser, nodePass);
    await waitForCreateService(configurator, 'nodes', nodeHost);

    await createZk(configurator, zkName, nodeHost);
    await waitContainersCreate(configurator, zkName);

    await createBk(configurator, zkName, bkName, nodeHost);
    await waitContainersCreate(configurator, bkName);
    await fileHelper(zkName, bkName);

    console.log(chalk.green('Services created!'));
  } catch (error) {
    console.log(
      chalk.red('Failed to create services, see the detailed error below:'),
    );
    console.log(error);
    throw new Error();
  }
};

exports.cleanServices = async configurator => {
  try {
    const file = fs.readFileSync('scripts/servicesApi/service.json');
    const json = JSON.parse(file);

    await jsonLoop(json, 'workers', cleanWk, configurator);
    await jsonLoop(json, 'brokers', cleanBk, configurator);
    await jsonLoop(json, 'zookeepers', cleanZk, configurator);
  } catch (error) {
    console.log(
      chalk.red('Failed to clean services, see the detailed error below:'),
    );
    console.log(error);
    throw new Error();
  }
};
