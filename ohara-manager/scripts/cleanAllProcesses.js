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

const cp = require('child_process');

/* eslint-disable no-console, no-process-exit */

// Kill both server and client processes
try {
  cp.execSync('./node_modules/.bin/forever stopall', { stdio: 'inherit' });
} catch (error) {
  throw new Error(error);
}

try {
  cp.execSync('pkill -f start.js', { stdio: 'inherit' });
} catch (error) {
  if (error.status === 1) {
    // Couldn't find any processes, exit with success status
    process.exit(0);
  }

  // Real error, throw it out
  throw new Error(error);
}
