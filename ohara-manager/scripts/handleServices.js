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
const chalk = require('chalk');

const api = require('../utils/apiHandler');

const zkName = 'zk' + api.randomName();
const bkName = 'bk' + api.randomName();

/* eslint-disable no-console */
async function createServices(
  configurator,
  nodeHost,
  nodePort,
  nodeUser,
  nodePass,
) {
  try {
    console.log(chalk.blue('Creating services for this test run'));

    await api.createNode(configurator, nodeHost, nodePort, nodeUser, nodePass);
    await api.waitCreate(configurator, 'nodes', nodeHost);
    await api.createZk(configurator, zkName, nodeHost);
    await api.waitCreate(configurator, 'zookeepers', zkName);
    await api.createBk(configurator, zkName, bkName, nodeHost);
    await api.waitCreate(configurator, 'brokers', bkName);
    await api.fileHelper(zkName, bkName);

    console.log(chalk.green('Successfully created all the services!'));
  } catch (err) {
    console.log(
      chalk.red('Failed to create services, see the detailed error blow:'),
    );
    console.log(err);
  }
}

/* eslint-disable no-console */
async function cleanServices(configurator, nodeHost) {
  const file = fs.readFileSync('scripts/servicesApi/service.json');
  var jsons = JSON.parse(file);
  try {
    console.log(
      chalk.blue(
        'Cleaning up all services since tests are finished! This might take a while...',
      ),
    );

    await api.jsonLoop(jsons, 'workers', api.cleanWk, configurator);
    await api.jsonLoop(jsons, 'brokers', api.cleanBk, configurator);
    await api.jsonLoop(jsons, 'zookeepers', api.cleanZk, configurator);
    await api.cleanNode(configurator, nodeHost);

    console.log(chalk.green('Successfully cleaned up all the services'));
  } catch (err) {
    console.log(
      chalk.red('Failed to create services, see the detailed error blow:'),
    );
    console.log(err);
  }
}

module.exports = {
  cleanServices,
  createServices,
};
