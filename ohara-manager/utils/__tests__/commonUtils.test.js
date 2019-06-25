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

const { randomPort } = require('../commonUtils');

describe('randomPort()', () => {
  it('returns a random port', () => {
    // Mock math.random so it always returns the same
    // possible port number: 11053
    global.Math.random = () => 0.1;
    expect(randomPort()).toBe(11053);
  });

  it('returns a random port according to the given min', () => {
    // 656
    global.Math.random = () => 0.01;
    expect(randomPort({ min: 1 })).toBe(656);
  });

  it('returns a random port according to the given max', () => {
    // 298
    global.Math.random = () => 0.99;
    expect(randomPort({ min: 1, max: 300 })).toBe(298);
  });
});
