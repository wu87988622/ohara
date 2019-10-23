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

import faker from 'faker';

const { system, random, lorem, internet, date } = faker;
const { commonFileName } = system;
const { uuid: id, number, alphaNumeric: revision } = random;
const { paragraph: message, word } = lorem;
const { domainName, ip, userName, url, password } = internet;

export const port = ({ min = 5000, max = 65535 } = {}) => {
  return Math.floor(Math.random() * (max - min + 1)) + min;
};

export const name = ({ length = 10, prefix } = {}) => {
  let name = '';
  const possible = 'abcdefghijklmnopqrstuvwxyz0123456789';

  for (let i = 0; i < length; i++) {
    name += possible.charAt(Math.floor(Math.random() * possible.length));
  }

  if (prefix) return `${prefix}${name}`;

  return name;
};

export const serverHost = () => {
  return 'http://' + window.location.hostname;
};

export {
  commonFileName,
  id,
  message,
  domainName,
  ip,
  userName,
  number,
  url,
  password,
  word,
  revision,
  date,
};
