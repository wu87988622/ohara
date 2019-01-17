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

const { exec } = require('child_process');

/* eslint-disable no-console */

// Stop all forever processes before running e2e tests
exec('./node_modules/.bin/forever stopall', (err, stdout) => {
  if (err) return;

  console.log(stdout.trim());
});

// Kill both server and client processes
exec('pkill -f index.js start.js', err => {
  if (err) return;
});
