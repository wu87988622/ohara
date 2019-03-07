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

const yargs = require('yargs');
const chalk = require('chalk');
const axios = require('axios');
const { isNumber } = require('lodash');

const getProjectVersion = require('./getProjectVersion');

/* eslint-disable no-process-exit, no-console */

const validateUrl = async url => {
  const regex = /https?:\/\/[-a-z0-9._+~#=:]{2,256}\/v[0-9]/gi;
  const validUrl = chalk.green('http://localhost:5050/v0');

  if (!regex.test(url)) {
    console.log(
      `
  --configurator: ${chalk.red(url)} is invalid

  The valid configurator URL should be something like: ${validUrl}
      `,
    );

    // Throw an error here won't stop the node process
    // since we're inside an async function, that's a promise rejection
    process.exit(1);
  }

  // Ensure the API URL can be reach
  try {
    await axios.get(`${url}/info`, { timeout: 3000 });
  } catch (err) {
    console.log(
      `
  --configurator: we're not able to connect to ${chalk.red(url)}

  Please make sure your Configurator is running at ${chalk.green(url)}
      `,
    );

    process.exit(1);
  }
};

const validatePort = port => {
  const isValidPort = port >= 1 && port <= 65535;

  if (!isNumber(port)) {
    throw Error('--port: can only accept number');
  }

  if (!isValidPort) {
    throw Error(
      `
  --port: ${chalk.red(port)} is invalid

  Valid port number is between the range of ${chalk.green(0)} and ${chalk.green(
        65535,
      )}
      `,
    );
  }
};

// Don't run yargs when running tests with jest
// Doing so will cause jest hanging in the watch mode
let getConfig;
if (process.env.NODE_ENV !== 'test') {
  getConfig = yargs
    .options({
      configurator: {
        demandOption: true,
        describe: 'Ohara configurator api',
        string: true,
        alias: 'c',
      },
      port: {
        describe: 'Ohara manager port, defaults to 5050',
        default: 5050,
        alias: 'p',
      },
    })
    .help()
    .alias('help', 'h')
    .version(getProjectVersion())
    .alias('version', 'v')
    .check(argv => {
      const { configurator, port } = argv;
      validateUrl(configurator);
      validatePort(port);
      return true;
    }).argv;
}

module.exports = {
  validateUrl,
  validatePort,
  getConfig,
};
