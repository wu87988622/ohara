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
import { hashByGroupAndName } from '../sha';

describe('hashByGroupAndName()', () => {
  it('same value should get same hash values', () => {
    const group = generate.serviceName();
    const name = generate.serviceName();
    expect(hashByGroupAndName(group, name)).toBe(
      hashByGroupAndName(group, name),
    );
  });

  it('group or name must provided', () => {
    const randomStr = generate.serviceName();
    expect(() => hashByGroupAndName(randomStr)).toThrow();
    expect(() => hashByGroupAndName(null, randomStr)).toThrow();
  });
});
