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

const execa = require('execa');
const yargs = require('yargs');
const chalk = require('chalk');

const mergeE2eReports = require('./mergeE2eReports');
const copyJars = require('./copyJars');
const { getConfig } = require('../utils/configHelpers');
const { waited } = require('./lib/waitOn');

const { configurator, port } = getConfig();
const { prod = false } = yargs.argv;

const run = async (prod, apiRoot, serverPort = 5050, clientPort = 3000) => {
  let server;
  let client;
  let cypress;

  // Start ohara manager server
  console.log(chalk.blue('Starting ohara manager server'));
  server = execa(
    'forever',
    ['start', 'index.js', '--configurator', apiRoot, '--port', port],
    {
      stdio: 'inherit',
    },
  );

  console.log('server.pid', server.pid);

  try {
    await server;
  } catch (err) {
    console.log(err.message);
    process.exit(1);
  }

  // Wait until the server is ready
  await waited(`http://localhost:${serverPort}`);

  // Start client server, this server only starts on local env not
  // on jenkins
  if (!prod) {
    console.log(chalk.blue('Starting ohara manager server'));
    client = execa(
      'forever',
      ['start', 'node_modules/react-scripts/scripts/start.js'],
      {
        cwd: 'client',
        stdio: 'inherit',
      },
    );
    console.log('client.pid', client.pid);

    try {
      await client;
    } catch (err) {
      console.log(err.message);
      process.exit(1);
    }

    // Wait until the client dev server is ready
    await waited(`http://localhost:${clientPort}`);
  }

  // Run e2e test
  console.log(chalk.blue('Running end to end tests with Cypress'));
  cypress = execa(
    'yarn',
    [
      'e2e:run',
      '--config',
      `baseUrl=http://localhost:${
        prod ? serverPort : clientPort
      },integrationFolder=cypress/api`,
    ],
    {
      cwd: 'client',
      stdio: 'inherit',
    },
  );
  console.log('cypress.pid', cypress.pid);

  const killSubProcess = () => {
    if (cypress) cypress.kill();
    if (client) client.kill();
    if (server) server.kill();
  };

  try {
    copyJars(); // We need these jars for test
    await cypress;
  } catch (err) {
    console.log(chalk.red(err.message));
  } finally {
    await mergeE2eReports('clientApi');
  }

  try {
    console.log(
      chalk.blue('Cleaning up all services. This might take a while...'),
    );

    killSubProcess();

    console.log(chalk.green('Successfully cleaned up all the services!'));
    process.exit(0);
  } catch (error) {
    console.log(
      chalk.red('Failed to clean services, see the detailed error below:'),
    );
    console.log(error);
  }
  process.exit(1);
};

run(prod, configurator, port);
