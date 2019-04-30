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

const api = require('../utils/apiHandler');
const fs = require('fs');

async function start(configurator, nodeHost) {
  const file = fs.readFileSync('scripts/servicesApi/service.json');
  var jsons = JSON.parse(file);
  await api.jsonLoop(jsons, 'workers', api.cleanWk, configurator);
  await api.jsonLoop(jsons, 'brokers', api.cleanBk, configurator);
  await api.jsonLoop(jsons, 'zookeepers', api.cleanZk, configurator);
  await api.cleanNode(configurator, nodeHost);
}

module.exports = { start };
