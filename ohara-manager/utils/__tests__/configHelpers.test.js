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
const axios = require('axios');

const { validateUrl, validatePort } = require('../configHelpers');

/* eslint-disable no-console */

jest.mock('axios');

jest.spyOn(process, 'exit').mockImplementation(number => number);
jest.spyOn(console, 'log').mockImplementation(str => str);
afterEach(jest.clearAllMocks);

describe('ValidatePort()', () => {
  it('exits on wrong data type', () => {
    validatePort('abc');
    expect(process.exit).toHaveBeenCalledWith(1);
    expect(console.log).not.toHaveBeenCalledTimes(0); // Should at least call console.log() one time
  });

  it('exits on port invalid port number', () => {
    validatePort(-1);
    expect(process.exit).toHaveBeenCalledWith(1);
    expect(console.log).not.toHaveBeenCalledTimes(0);

    validatePort(999999);
    expect(process.exit).toHaveBeenCalledWith(1);
    expect(console.log).not.toHaveBeenCalledTimes(0);
  });

  it('works when given the correct port', () => {
    validatePort(1234);
    expect(process.exit).not.toBeCalled();
    expect(console.log).not.toBeCalled();
  });
});

describe('validateUrl()', () => {
  // TODO: test the fail cases
  it('works when given the correct URL', async () => {
    const res = { data: '' };
    axios.get.mockResolvedValue(res);

    await validateUrl('http://localhost:5050/v0');
    expect(process.exit).not.toBeCalled();
    expect(console.log).not.toHaveBeenCalledTimes(0);
  });
});
