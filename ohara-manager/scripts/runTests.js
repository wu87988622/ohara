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

const execa = require('execa');
const yargs = require('yargs');
const chalk = require('chalk');
const _ = require('lodash');
const fs = require('fs');

const mergeReports = require('./mergeReports');
const copyJars = require('./copyJars');
const utils = require('./scriptsUtils');
const commonUtils = require('../utils/commonUtils');
const { getConfig } = require('../utils/configHelpers');

const { configurator, port } = getConfig();
const {
  testMode,
  ci = false,
  nodeHost,
  nodePort,
  nodeUser,
  nodePass,
  servicePrefix,
} = yargs.argv;

const getDefaultEnv = () => {
  const filePath = './client/cypress.env.json';
  if (fs.existsSync(filePath)) {
    return JSON.parse(fs.readFileSync(filePath));
  }

  return {};
};

/* eslint-disable no-process-exit, no-console */
const run = async (ci, apiRoot, serverPort = 5050, clientPort = 3000) => {
  let server;
  let client;
  let cypress;
  serverPort = serverPort === 0 ? commonUtils.randomPort() : serverPort;

  const defaultEnv = getDefaultEnv();
  const prefix = servicePrefix ? servicePrefix : defaultEnv.servicePrefix;

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
    const serverProcess = await server;
    console.log(`server output: ${serverProcess.stdout}`);
  } catch (err) {
    console.log(err.message);
    process.exit(1);
  }

  // Wait until the server is ready
  await utils.waitOnService(`http://localhost:${serverPort}`);

  // Start client server for generating instrument code
  // by instrument-cra plugin
  // This instrument plugin is only available in the development environment!
  // https://github.com/cypress-io/instrument-cra/issues/135
  if (ci && testMode !== 'api') {
    console.log(chalk.blue(`Starting client server`));
    client = execa(
      'forever',
      [
        'start',
        '-c',
        'node -r @cypress/instrument-cra',
        'node_modules/react-scripts/scripts/start.js',
      ],
      {
        cwd: 'client',
        stdio: 'inherit',
        env: {
          BROWSER: 'none',
        },
      },
    );

    try {
      const clientProcess = await client;
      console.log(`client output: ${clientProcess.stdout}`);
    } catch (err) {
      console.log(err.message);
      process.exit(1);
    }

    // Wait until the client dev server is ready
    await utils.waitOnService(`http://localhost:${clientPort}`);
  }

  const buildCypressEnv = () => {
    const env = [];
    env.push(`port=${ci ? clientPort : serverPort}`);

    if (nodeHost) {
      env.push(`nodeHost=${nodeHost}`);
    }
    if (nodePort) {
      env.push(`nodePort=${nodePort}`);
    }
    if (nodeUser) {
      env.push(`nodeUser=${nodeUser}`);
    }
    if (nodePass) {
      env.push(`nodePass=${nodePass}`);
    }
    if (prefix) {
      env.push(`servicePrefix=${prefix}`);
    }

    return env.join(',');
  };

  // Run test
  console.log(chalk.blue(`Running ${testMode} tests with Cypress`));
  cypress = execa(
    'yarn',
    [
      `test:${testMode}:run`,
      '--config',
      `baseUrl=http://localhost:${ci ? clientPort : serverPort}`,
      '--env',
      buildCypressEnv(),
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
    await mergeReports(`client${_.capitalize(testMode)}`);
  }

  try {
    console.log(
      chalk.blue('Cleaning up all services. This might take a while...'),
    );

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
if (!utils.checkClientBuildDir(testMode)) {
  process.exit(1);
}

run(ci, configurator, port);
