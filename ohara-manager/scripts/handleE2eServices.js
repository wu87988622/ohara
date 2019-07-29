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
const { get, isUndefined } = require('lodash');

const commonUtils = require('../utils/commonUtils');
const utils = require('./scriptsUtils');

/* eslint-disable no-console */
const randomServiceName = () => {
  const possible = 'abcdefghijklmnopqrstuvwxyz0123456789';
  let name = '';

  for (let i = 0; i < 5; i++) {
    name += possible.charAt(Math.floor(Math.random() * possible.length));
  }

  return name;
};

const writeServiceInfoToFile = (zookeeperClusterName, brokerClusterName) => {
  const filePath = 'scripts/servicesApi/service.json';

  fs.access(filePath, function(error) {
    if (error) {
      fs.mkdirSync('scripts/servicesApi');
    }

    const zookeeper = {
      name: zookeeperClusterName,
      serviceType: 'zookeepers',
    };

    const broker = {
      name: brokerClusterName,
      serviceType: 'brokers',
    };

    const data = JSON.stringify([zookeeper, broker]);
    fs.writeFile(filePath, data, error => {
      error;
    });
  });
};

exports.getDefaultEnv = () => {
  const filePath = './client/cypress.env.json';
  if (fs.existsSync(filePath)) {
    return JSON.parse(fs.readFileSync(filePath));
  }

  return {};
};

const isServiceReady = async apiUrl => {
  const response = await axios.get(apiUrl);
  const serviceIsReady = get(response, 'data.state', 'FAILED');
  return serviceIsReady === 'RUNNING';
};

const isRemovedFromList = async (url, serviceName) => {
  const response = await axios.get(`${url}`);
  const services = get(response, 'data', []);

  // If there's nothing returned from the request,
  // returns early at this point
  if (services.length === 0) return true;

  const hasService = service => service.name === serviceName;
  const result = services.some(hasService);
  return !result;
};

const createNode = async ({
  apiRoot,
  nodeHost,
  nodePort,
  nodeUser,
  nodePass,
}) => {
  const nodeUrl = `${apiRoot}/nodes`;

  await axios.post(nodeUrl, {
    name: nodeHost,
    port: nodePort,
    user: nodeUser,
    password: nodePass,
  });

  const isNodeReady = async () => {
    const response = await axios.get(nodeUrl);
    const result = response.data.some(service => service.name == nodeHost);
    return result;
  };

  await utils.waitUntil({ condition: isNodeReady });
};

exports.createServices = async ({
  configurator: apiRoot,
  nodeHost,
  nodePort,
  nodeUser,
  nodePass,
}) => {
  console.log(chalk.blue('Creating services for this test run'));

  const zookeeperClusterName = 'zk' + randomServiceName();
  const brokerClusterName = 'bk' + randomServiceName();

  try {
    await createNode({ apiRoot, nodeHost, nodePort, nodeUser, nodePass });

    const zookeeperUrl = `${apiRoot}/zookeepers`;
    const brokerUrl = `${apiRoot}/brokers`;

    await axios.post(zookeeperUrl, {
      clientPort: commonUtils.randomPort(),
      electionPort: commonUtils.randomPort(),
      peerPort: commonUtils.randomPort(),
      name: zookeeperClusterName,
      nodeNames: [nodeHost],
    });

    await axios.put(`${zookeeperUrl}/${zookeeperClusterName}/start`);

    await utils.waitUntil({
      condition: () =>
        isServiceReady(`${zookeeperUrl}/${zookeeperClusterName}`),
    });

    await axios.post(brokerUrl, {
      clientPort: commonUtils.randomPort(),
      exporterPort: commonUtils.randomPort(),
      jmxPort: commonUtils.randomPort(),
      zookeeperClusterName: zookeeperClusterName,
      name: brokerClusterName,
      nodeNames: [nodeHost],
    });

    await axios.put(`${brokerUrl}/${brokerClusterName}/start`);

    await utils.waitUntil({
      condition: () => isServiceReady(`${brokerUrl}/${brokerClusterName}`),
    });

    await writeServiceInfoToFile(zookeeperClusterName, brokerClusterName);

    console.log(chalk.green('Services created!'));
  } catch (error) {
    console.log(
      chalk.red('Failed to create services, see the detailed error below:'),
    );
    console.log(error);
    throw new Error();
  }
};

const isServiceStopped = async apiUrl => {
  const zookeeper = await axios.get(apiUrl);
  const zookeeperState = get(zookeeper, 'data.state', 'RUNNING');
  return isUndefined(zookeeperState);
};

const cleanWorkers = async (workers, apiRoot) => {
  await Promise.all(
    workers.map(async worker => {
      const { name: serviceName, serviceType } = worker;
      const url = `${apiRoot}/${serviceType}`;
      await axios.delete(`${url}/${serviceName}`);
      const condition = () => isRemovedFromList(url, serviceName);

      await utils.waitUntil({ condition });
    }),
  );
};

const deleteServices = async (services, apiRoot) => {
  await Promise.all(
    services.map(async service => {
      const { name: serviceName, serviceType } = service;
      const apiUrl = `${apiRoot}/${serviceType}`;

      await axios.put(`${apiUrl}/${serviceName}/stop`);
      await utils.waitUntil({ condition: () => isServiceStopped(apiUrl) });
      await axios.delete(`${apiUrl}/${serviceName}`);
    }),
  );
};

const getByServiceType = services => {
  let workers = [];
  let brokers = [];
  let zookeepers = [];

  services.forEach(service => {
    if (service.serviceType === 'workers') workers.push(service);
    if (service.serviceType === 'brokers') brokers.push(service);
    if (service.serviceType === 'zookeepers') zookeepers.push(service);
  });

  return { workers, brokers, zookeepers };
};

exports.cleanServices = async apiRoot => {
  try {
    const file = fs.readFileSync('scripts/servicesApi/service.json');
    const services = JSON.parse(file);
    const { workers, brokers, zookeepers } = getByServiceType(services);

    // 1. Note that the deleting order matters, it should goes with
    // workers -> brokers -> zookeepers
    // 2. We're not deleting node we created early since it can be
    // deleted safely with Jenkins

    await cleanWorkers(workers, apiRoot);
    await deleteServices(brokers, apiRoot);
    await deleteServices(zookeepers, apiRoot);
  } catch (error) {
    console.log(
      chalk.red('Failed to clean services, see the detailed error below:'),
    );
    console.log(error);
    throw new Error();
  }
};
