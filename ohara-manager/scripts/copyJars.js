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

const fs = require('fs');
const execa = require('execa');

const srcBase = '../ohara-it/build/libs';
const distBase = './client/cypress/fixtures/jars';
const files = [
  'ohara-it-source.jar',
  'ohara-it-sink.jar',
  'ohara-it-stream.jar',
  'ohara-it-0.11.0-SNAPSHOT.jar',
];

/* eslint-disable no-process-exit, no-console */
const buildJars = execa('./gradlew', ['ohara-it:jar'], {
  cwd: '..',
  stdio: 'inherit',
});

buildJars
  .then((value) => {
    console.log(`build jars output: ${value.stdout}`);

    if (!fs.existsSync(`${distBase}`)) {
      fs.mkdirSync(`${distBase}`, { recursive: true });
    }

    files.forEach((file) => {
      fs.copyFileSync(`${srcBase}/${file}`, `${distBase}/${file}`);
    });
  })
  .catch((err) => {
    console.log(err.message);
    process.exit(1);
  });
