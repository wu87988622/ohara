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
const { getConfig } = require('../utils/configHelpers');
const { waited } = require('./lib/waitOn');
const mergeTestReports = require('./mergeE2eReports');

const { configurator, port } = getConfig;

const { prod = false, nodeHost, nodePort, nodeUser, nodePass } = yargs.argv;

debug('prod: ', prod);
debug('configurator: ', configurator);
debug('port: ', port);
debug('nodeHost: ', nodeHost || 'Not input.');
debug('nodePort: ', nodePort || 'Not input.');
debug('nodeUser: ', nodeUser || 'Not input.');
debug('nodePass: ', nodePass || 'Not input.');

function debug(...message) {
  console.log(...message);
}

const run = async (prod, apiRoot, serverPort = 5050, clientPort = 3000) => {
  let server;
  let client;
  let cypress;

  const killSubProcess = () => {
    debug('========= Kill all subProcess =========');
    if (cypress) cypress.kill();
    if (client) client.kill();
    if (server) server.kill();
  };

  // Start server
  debug('========= Start server =========');
  server = execa(
    'forever',
    ['start', 'index.js', '--configurator', apiRoot, '--port', port],
    {
      stdio: 'inherit',
    },
  );
  debug('server.pid', server.pid);
  try {
    await server;
  } catch (err) {
    debug(err.message);
    process.exit(1);
  }

  // Waiting for server
  await waited(`http://localhost:${serverPort}`);

  // Start client
  if (!prod) {
    debug('========= Start client =========');
    client = execa(
      'forever',
      ['start', 'node_modules/react-scripts/scripts/start.js'],
      {
        cwd: 'client',
        stdio: 'inherit',
      },
    );
    debug('client.pid', client.pid);
    try {
      await client;
    } catch (err) {
      debug(err.message);
      process.exit(1);
    }

    // Waiting for client
    await waited(`http://localhost:${clientPort}`);
  }

  const buildCypressEnv = () => {
    const env = [];
    env.push(`port=${prod ? serverPort : clientPort}`);
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
    return env.join(',');
  };

  // Run e2e test
  debug('========= Run e2e test =========');
  cypress = execa(
    'yarn',
    [
      'e2e:run',
      '--config',
      `baseUrl=http://localhost:${prod ? serverPort : clientPort}`,
      '--env',
      buildCypressEnv(),
    ],
    {
      cwd: 'client',
      stdio: 'inherit',
    },
  );
  debug('cypress.pid', cypress.pid);

  try {
    await cypress;
  } catch (err) {
    debug(err.message);
    await mergeTestReports();
    killSubProcess();
    process.exit(1);
  } finally {
    await mergeTestReports();
    killSubProcess();
    process.exit(0);
  }
};

run(prod, configurator, port);
