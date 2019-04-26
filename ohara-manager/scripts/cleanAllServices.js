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
const axios = require('axios');
const fs = require('fs');

const { configurator, port, nodeHost } = yargs.argv;

debug('configurator: ', configurator);
debug('port: ', port);
debug('nodeHost: ', nodeHost || 'Not input.');

function debug(...message) {
  console.log(...message);
}

const cleanServices = async () => {
  const file = fs.readFileSync('scripts/env/service.json');
  var bk = JSON.parse(file).bk;
  var zk = JSON.parse(file).zk;
  await cleanBk(bk);
  await wait('brokers');
  await cleanZk(zk);
  await wait('zookeepers');
  await cleanNode();
};

cleanServices();

async function cleanNode() {
  await axios.delete(`http://${configurator}:${port}/v0/nodes/${nodeHost}`);
}

async function cleanZk(name) {
  await axios.delete(`http://${configurator}:${port}/v0/zookeepers/${name}`);
}

async function cleanBk(name) {
  await axios.delete(`http://${configurator}:${port}/v0/brokers/${name}`);
}

async function wait(api) {
  const res = await axios.get(`http://${configurator}:${port}/v0/` + api);
  if (res.data.length > 0) {
    sleep(1000);
    await wait(api);
  }

  return;
}

function sleep(milliseconds) {
  var start = new Date().getTime();
  for (var i = 0; i < 1e7; i++) {
    if (new Date().getTime() - start > milliseconds) {
      break;
    }
  }
}
