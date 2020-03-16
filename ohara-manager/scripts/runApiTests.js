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
const chalk = require('chalk');

const mergeReports = require('./mergeReports');
const copyJars = require('./copyJars');
const utils = require('./scriptsUtils');
const commonUtils = require('../utils/commonUtils');
const { getConfig } = require('../utils/configHelpers');

const { configurator, port } = getConfig();

const run = async (apiRoot, serverPort = 5050) => {
  let server;
  let cypress;

  serverPort = serverPort === 0 ? commonUtils.randomPort() : serverPort;

  // Start ohara manager server
  console.log(chalk.blue('Starting ohara manager server'));
  server = execa(
    'forever',
    ['start', 'start.js', '--configurator', apiRoot, '--port', serverPort],
    {
      stdio: 'inherit',
    },
  );

  console.log('server.pid', server.pid);

  try {
    const process = await server;
    console.log(`server output: ${process.stdout}`);
  } catch (err) {
    console.log(err.message);
    process.exit(1);
  }

  // Wait until the server is ready
  await utils.waitOnService(`http://localhost:${serverPort}`);

  // Run api test
  console.log(chalk.blue('Running API tests with Cypress'));
  cypress = execa(
    'yarn',
    ['test:api:run', '--config', `baseUrl=http://localhost:${serverPort}`],
    {
      cwd: 'client',
      stdio: 'inherit',
    },
  );
  console.log('cypress.pid', cypress.pid);

  const killSubProcess = () => {
    if (cypress) cypress.kill();
    if (server) server.kill();
  };

  try {
    copyJars(); // We need these jars for test
    await cypress;
  } catch (err) {
    console.log(chalk.red(err.message));
  } finally {
    await mergeReports('clientApi');
  }

  try {
    killSubProcess();

    console.log(chalk.green('Successfully cleaned up all the services!'));
    process.exit(0);
  } catch (error) {
    // Ignore the error message, and exit with a fail status
    process.exit(1);
  }
};

// Do not run the test if the build dir is not present
// as this will cause the script to fail silently
if (!utils.checkClientBuildDir()) {
  process.exit(1);
}

run(configurator, port);
