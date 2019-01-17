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

const _ = require('../helpers');

describe('isEmptyStr', () => {
  it('should return turn if the given value is an empty string', () => {
    expect(_.isEmptyStr('')).toBe(true);
  });

  it('should return false if the given value is not an empty string', () => {
    expect(_.isEmptyStr('xyz')).toBe(false);
  });
});

describe('isNumber', () => {
  it('should return true if the given value is a number', () => {
    expect(_.isNumber(123)).toBe(true);
  });

  it('should return false if the given value is not a number', () => {
    expect(_.isNumber([])).toBe(false);
    expect(_.isNumber({})).toBe(false);
    expect(_.isNumber(null)).toBe(false);
    expect(_.isNumber(() => {})).toBe(false);
    expect(_.isNumber('test')).toBe(false);
    expect(_.isNumber(false)).toBe(false);
  });
});
