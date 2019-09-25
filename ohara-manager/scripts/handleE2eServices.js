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
const { get, isNull } = require('lodash');

const utils = require('./scriptsUtils');

/* eslint-disable no-console */
const randomServiceName = ({ length = 5, prefix }) => {
  const possible = 'abcdefghijklmnopqrstuvwxyz0123456789';
  let name = '';

  for (let i = 0; i < length; i++) {
    name += possible.charAt(Math.floor(Math.random() * possible.length));
  }

  return `${prefix}${name}`;
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
  servicePrefix,
}) => {
  console.log(chalk.blue('Creating services for this test run'));

  const zookeeperClusterName = `${servicePrefix}${randomServiceName({
    prefix: 'zk',
  })}`;
  const brokerClusterName = `${servicePrefix}${randomServiceName({
    prefix: 'bk',
  })}`;
  const workerClusterName = `${servicePrefix}${randomServiceName({
    prefix: 'wk',
  })}`;

  try {
    await createNode({ apiRoot, nodeHost, nodePort, nodeUser, nodePass });

    const zookeeperUrl = `${apiRoot}/zookeepers`;
    const brokerUrl = `${apiRoot}/brokers`;
    const workerUrl = `${apiRoot}/workers`;

    await axios.post(zookeeperUrl, {
      name: zookeeperClusterName,
      nodeNames: [nodeHost],
    });

    await axios.put(`${zookeeperUrl}/${zookeeperClusterName}/start`);

    await utils.waitUntil({
      condition: () =>
        isServiceReady(`${zookeeperUrl}/${zookeeperClusterName}`),
    });

    await axios.post(brokerUrl, {
      name: brokerClusterName,
      zookeeperClusterName,
      nodeNames: [nodeHost],
    });

    await axios.put(`${brokerUrl}/${brokerClusterName}/start`);

    await utils.waitUntil({
      condition: () => isServiceReady(`${brokerUrl}/${brokerClusterName}`),
    });

    await axios.post(workerUrl, {
      name: workerClusterName,
      brokerClusterName,
      nodeNames: [nodeHost],
      tags: {
        broker: {
          name: brokerClusterName,
        },
        zookeeper: {
          name: zookeeperClusterName,
        },
      },
    });

    await axios.put(`${workerUrl}/${workerClusterName}/start`);

    await utils.waitUntil({
      condition: () => isServiceReady(`${workerUrl}/${workerClusterName}`),
    });

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
  const service = await axios.get(apiUrl);
  const serviceState = get(service, 'data.state', null);
  return isNull(serviceState);
};

const deleteServices = async (services, apiRoot) => {
  const { serviceType, serviceNames } = services;

  for (let serviceName of serviceNames) {
    const apiUrl = `${apiRoot}/${serviceType}`;
    await axios.put(`${apiUrl}/${serviceName}/stop?force=true`);

    await utils.waitUntil({
      condition: () => isServiceStopped(`${apiUrl}/${serviceName}`),
    });

    await axios.delete(`${apiUrl}/${serviceName}`);
  }
};

const getByServiceType = (services, servicePrefix) => {
  let workers = { serviceType: 'workers', serviceNames: [] };
  let brokers = { serviceType: 'brokers', serviceNames: [] };
  let zookeepers = { serviceType: 'zookeepers', serviceNames: [] };

  services.forEach(service => {
    if (service.settings.name.includes(servicePrefix)) {
      workers.serviceNames.push(service.name);
    }

    if (service.settings.tags.broker) {
      brokers.serviceNames.push(service.settings.tags.broker.name);
    }

    if (service.settings.tags.zookeeper) {
      zookeepers.serviceNames.push(service.settings.tags.zookeeper.name);
    }
  });

  return { workers, brokers, zookeepers };
};

exports.cleanServices = async (apiRoot, nodeName, servicePrefix) => {
  try {
    const workersRes = await axios.get(`${apiRoot}/workers`);

    const { workers, brokers, zookeepers } = getByServiceType(
      workersRes.data,
      servicePrefix,
    );

    // Note that the deleting order matters, it should goes like:
    // workers -> brokers -> zookeepers -> node

    await deleteServices(workers, apiRoot);
    await deleteServices(brokers, apiRoot);
    await deleteServices(zookeepers, apiRoot);

    //get last broker and zookeepers
    const brokersRes = await axios.get(`${apiRoot}/brokers`);
    const zookeepersRes = await axios.get(`${apiRoot}/zookeepers`);
    const lastBrokers = {
      serviceType: 'brokers',
      serviceNames: brokersRes.data.map(bk => bk.name),
    };
    const lastZookeepers = {
      serviceType: 'zookeepers',
      serviceNames: zookeepersRes.data.map(zk => zk.name),
    };
    await deleteServices(lastBrokers, apiRoot);
    await deleteServices(lastZookeepers, apiRoot);

    await axios.delete(`${apiRoot}/nodes/${nodeName}`);
  } catch (error) {
    console.log(
      chalk.red('Failed to clean services, see the detailed error below:'),
    );
    console.log(error);
    throw new Error();
  }
};
