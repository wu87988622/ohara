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

import { isEmpty } from 'lodash';

import validate from '../validate';
import * as generate from 'utils/generate';

describe('validate()', () => {
  it('returns an empty error object if there is no error found during validation', () => {
    const values = {
      name: generate.name(),
      port: generate.port(),
      user: generate.userName(),
      password: generate.password(),
    };

    const result = validate(values);

    expect(isEmpty(result)).toBe(true);
  });

  it('returns validation error message for each field', () => {
    const values = {
      name: '',
      port: '',
      user: '',
      password: '',
    };
    const requiredField = 'Required field';
    const result = validate(values);

    expect(result.name).toBe(requiredField);
    expect(result.port).toBe(requiredField);
    expect(result.user).toBe(requiredField);
    expect(result.password).toBe(requiredField);
  });
});
