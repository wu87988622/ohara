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

import * as generate from '../generate';
import { required, validServiceName, lessThanTweenty } from '../validate';

describe('required()', () => {
  it('returns `undefined` if the value is given', () => {
    expect(required(generate.userName())).toBeUndefined();
  });

  it('reutuns the error message if the given value is falsy', () => {
    expect(required('')).toBe('Required');
    expect(required(undefined)).toBe('Required');
  });
});

describe('validServiceName()', () => {
  it('returns `undefined` if the given value is valid', () => {
    // generate.serviceName is a custom function that returns
    // "valid" service name
    expect(validServiceName(generate.serviceName())).toBeUndefined();
  });

  it('returns the error message if the given value is not vaild', () => {
    const error = 'You only can use lower case letters and numbers';
    expect(validServiceName('ABC')).toBe(error);
    expect(validServiceName('!#@$%^&')).toBe(error);
    expect(validServiceName(' ')).toBe(error);
  });
});

describe('lessThanTweenty()', () => {
  it('returns `undefined` if the given value length is less than 20', () => {
    expect(
      lessThanTweenty(generate.serviceName({ length: 20 })),
    ).toBeUndefined();
  });

  it('returns the error message if the given value is greater than 20', () => {
    const error = 'Must be between 1 and 20 characters long';
    expect(lessThanTweenty(generate.serviceName({ length: 21 }))).toBe(error);
    expect(lessThanTweenty(generate.serviceName({ length: 100 }))).toBe(error);
  });
});
