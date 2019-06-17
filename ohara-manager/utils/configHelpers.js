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
const utils = require('./commonUtils');

/* eslint-disable no-process-exit, no-console */

const validateUrl = async url => {
  const regex = /https?:\/\/[-a-z0-9._+~#=:]{2,256}\/v[0-9]/gi;
  const validUrl = chalk.bold.green('http://localhost:5050/v0');

  if (!regex.test(url)) {
    console.log();
    console.log(
      `--configurator: ${chalk.bold.red(
        url,
      )} is invalid\n\nThe valid configurator URL should be something like: ${validUrl}`,
    );
    console.log();

    // Throw an error here won't stop the node process
    // since we're inside an async function, that's a promise rejection
    process.exit(1);
  }

  // Ensure the API URL can be reach
  // Retry on failure
  const makeRequest = () => axios.get(`${url}/info`, { timeout: 10000 });
  const printSuccessMsg = () =>
    console.log(
      chalk.green(
        `Successfully connected to the configurator: ${chalk.bold.underline(
          url,
        )}`,
      ),
    );

  const printFailMsg = error => {
    console.log(
      chalk.yellow(
        `Couldn't connect to the configurator url you provided: ${chalk.bold(
          url,
        )}, retrying...`,
      ),
    );

    console.log(chalk.bold.yellow(error.message));
  };

  let count = 0;
  const retryConnection = async () => {
    // The total waiting time : 10 (count) * 3000 (sleep) = 30000 ms = 30 seconds,
    // the actual time should be more since we have a 10 seconds timeout setting
    // in axios

    if (count >= 10) {
      console.log();
      console.log(
        `--configurator: we're not able to connect to ${chalk.bold.red(
          url,
        )}\n\nPlease make sure your Configurator is running at ${chalk.bold.green(
          url,
        )}`,
      );
      console.log();

      process.exit(1);
    }

    count++;

    await utils.sleep(3000); // wait for 3 sec and make another request

    try {
      await makeRequest();
      printSuccessMsg();
    } catch (error) {
      printFailMsg(error);
      await retryConnection();
    }
  };

  try {
    await makeRequest();
    printSuccessMsg();
  } catch (error) {
    printFailMsg(error);
    await retryConnection();
  }
};

const validatePort = port => {
  const isValidPort = port >= 1 && port <= 65535;

  if (!isNumber(port)) {
    console.log();
    console.log(`--port: ${chalk.red(port)} is not a number`);
    console.log();
    process.exit(1);
  }

  if (!isValidPort) {
    console.log();
    console.log(
      `--port: ${chalk.red(
        port,
      )} is invalid\n\nValid port number is between the range of ${chalk.green(
        0,
      )} and ${chalk.green(65535)}`,
    );
    console.log();
    process.exit(1);
  }
};

const getConfig = () => {
  // Don't run yargs when running tests with jest
  // Doing so will cause jest hanging in the watch mode
  if (process.env.NODE_ENV !== 'test') {
    return yargs
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
};

module.exports = {
  validateUrl,
  validatePort,
  getConfig,
};
