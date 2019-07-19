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
const { get } = require('lodash');

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

const isServiceReady = async (apiRoot, serviceName) => {
  const res = await axios.get(`${apiRoot}/containers/${serviceName}`);
  const containerIsReady = get(res, 'data[0].containers[0].state', undefined);
  return Boolean(containerIsReady);
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

    await axios.post(`${apiRoot}/zookeepers`, {
      clientPort: commonUtils.randomPort(),
      electionPort: commonUtils.randomPort(),
      peerPort: commonUtils.randomPort(),
      name: zookeeperClusterName,
      nodeNames: [nodeHost],
    });

    await axios.put(`${apiRoot}/zookeepers/${zookeeperClusterName}/start`);

    await utils.waitUntil({
      condition: () => isServiceReady(apiRoot, zookeeperClusterName),
    });

    await axios.post(`${apiRoot}/brokers`, {
      clientPort: commonUtils.randomPort(),
      exporterPort: commonUtils.randomPort(),
      jmxPort: commonUtils.randomPort(),
      zookeeperClusterName: zookeeperClusterName,
      name: brokerClusterName,
      nodeNames: [nodeHost],
    });

    await utils.waitUntil({
      condition: () => isServiceReady(apiRoot, brokerClusterName),
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

const cleanBrokers = async (brokers, apiRoot) => {
  await Promise.all(
    brokers.map(async broker => {
      const { name: serviceName, serviceType } = broker;
      const url = `${apiRoot}/${serviceType}`;
      await axios.delete(`${url}/${serviceName}`);
      const condition = () => isRemovedFromList(url, serviceName);

      await utils.waitUntil({ condition });
    }),
  );
};

const cleanZookeepers = async (zookeepers, apiRoot) => {
  await Promise.all(
    zookeepers.map(async zookeeper => {
      const { name, serviceType } = zookeeper;
      const url = `${apiRoot}/${serviceType}`;

      await axios.put(`${url}/${name}/stop`);
      const zookeeperCondition = async () => {
        const zookeeper = await axios.get(`${url}/${name}`);
        const zookeeperState = get(zookeeper, 'data.state', 'stop');
        return zookeeperState === 'stop';
      };

      await utils.waitUntil({ condition: zookeeperCondition });
      await axios.delete(`${url}/${name}`);
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

    await cleanWorkers(workers, apiRoot);
    await cleanBrokers(brokers, apiRoot);
    await cleanZookeepers(zookeepers, apiRoot);
  } catch (error) {
    console.log(
      chalk.red('Failed to clean services, see the detailed error below:'),
    );
    console.log(error);
    throw new Error();
  }
};
