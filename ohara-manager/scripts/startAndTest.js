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
const waitOn = require('wait-on');
const yargs = require('yargs');
const { getConfig } = require('../utils/configHelpers');

const { configurator, port } = getConfig;

const { prod = false } = yargs.argv;

debug('Production: ', prod);
debug('Configurator: ', configurator);
debug('Port: ', port);

function debug(...message) {
  console.log(...message);
}

const waited = url =>
  new Promise((resolve, reject) => {
    debug('starting waitOn %s', url);
    waitOn(
      {
        resources: Array.isArray(url) ? url : [url],
        // delay: 10000, // 10s
        interval: 2000, // 2s
        window: 1000,
        log: true,
      },
      err => {
        if (err) {
          debug('error waiting for url', url);
          debug(err.message);
          return reject(err);
        }
        debug('waitOn finished successfully');
        resolve();
      },
    );
  });

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

  // Run e2e test
  debug('========= Run e2e test =========');
  cypress = execa(
    'yarn',
    [
      'e2e:run',
      '--config',
      `baseUrl=http://localhost:${prod ? serverPort : clientPort}`,
      '--env',
      `port=${prod ? serverPort : clientPort}`,
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
    killSubProcess();
    process.exit(1);
  } finally {
    killSubProcess();
    process.exit(0);
  }
};

run(prod, configurator, port);
