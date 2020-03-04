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
import {
  required,
  validServiceName,
  minLength,
  maxLength,
  minNumber,
  maxNumber,
  checkDuplicate,
} from '../validate';

describe('required()', () => {
  it('returns `undefined` if the value is given', () => {
    expect(required(generate.userName())).toBeUndefined();
  });

  it('returns the error message if the given value is falsy', () => {
    expect(required('')).toBe('This is a required field');
    expect(required(undefined)).toBe('This is a required field');
  });
});

describe('validServiceName()', () => {
  it('returns `undefined` if the given value is valid', () => {
    // generate.serviceName is a custom function that returns
    // "valid" service name
    expect(validServiceName(generate.serviceName())).toBeUndefined();
  });

  it('returns the error message if the given value is not valid', () => {
    const error = 'You only can use lower case letters and numbers';
    expect(validServiceName('ABC')).toBe(error);
    expect(validServiceName('!#@$%^&')).toBe(error);
    expect(validServiceName(' ')).toBe(error);
  });
});

describe('minLength()', () => {
  const min = minLength(50);
  it('returns `undefined` if the given value length is greater than or equal to or equal to 50', () => {
    expect(min(generate.randomString({ length: 51 }))).toBeUndefined();
    expect(min(generate.randomString({ length: 50 }))).toBeUndefined();
  });

  it('returns the error message if the given value is less than or equal to 50', () => {
    expect(min(generate.randomString({ length: 49 }))).toBe(
      'The value must be greater than or equal to 50 characters long',
    );
  });
});

describe('maxLength()', () => {
  const max = maxLength(50);
  it('returns `undefined` if the given value length is less than or equal to 50', () => {
    expect(max(generate.randomString({ length: 50 }))).toBeUndefined();
  });

  it('returns the error message if the given value is greater than or equal to 50', () => {
    expect(max(generate.randomString({ length: 51 }))).toBe(
      'The value must be less than or equal to 50 characters long',
    );
  });
});

describe('minNumber()', () => {
  const min = minNumber(50);
  it('returns `undefined` if the given value is greater than or equal to 50', () => {
    expect(min(51)).toBeUndefined();
    expect(min(50)).toBeUndefined();
  });

  it('returns an error message if the given value is less than or equal to 50', () => {
    expect(min(49)).toBe('The value must be greater than or equal to 50');
  });
});

describe('maxNumber()', () => {
  const max = maxNumber(50);
  it('returns `undefined` if the given value is less than or equal to 50', () => {
    expect(max(50)).toBeUndefined();
  });

  it('returns the error message if the given value is greater than or equal to 50', () => {
    expect(max(51)).toBe('The value must be less than or equal to 50');
  });
});

describe('checkDuplicate()', () => {
  let list;
  it('returns `undefined` if the list is empty or not array', () => {
    expect(checkDuplicate(null)(123)).toBeUndefined();
    expect(checkDuplicate('fake')(456)).toBeUndefined();
    expect(checkDuplicate(undefined)('abc')).toBeUndefined();
    expect(checkDuplicate(list)([])).toBeUndefined();
  });
  const list2 = [123, 'foo', 'bar'];
  it('returns `undefined` if the number or string is not in list', () => {
    expect(checkDuplicate(list2)(123)).toBe('The value 123 is in used');
    expect(checkDuplicate(list2)(456)).toBeUndefined();
    expect(checkDuplicate(list2)('foo')).toBe('The value foo is in used');
  });
});
