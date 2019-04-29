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

const yargs = require('yargs');
const api = require('../utils/apiHandler');
const fs = require('fs');

const { configurator, nodeHost } = yargs.argv;

debug('configurator: ', configurator);
debug('nodeHost: ', nodeHost || 'Not input.');

function debug(...message) {
  console.log(...message);
}

const cleanServices = async () => {
  const file = fs.readFileSync('scripts/env/service.json');
  var bk = JSON.parse(file).bk;
  var zk = JSON.parse(file).zk;
  await api.cleanBk(configurator, bk);
  await api.waitDelete(configurator, 'brokers');
  await api.cleanZk(configurator, zk);
  await api.waitDelete(configurator, 'zookeepers');
  await api.cleanNode(configurator, nodeHost);
};

cleanServices();
