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

import { createBody } from '../definitionsUtils';

describe('createBody - positive numbers', () => {
  it('expect POSITIVE_SHORT to be positiveNumber type', () => {
    const params = {
      key: {
        internal: false,
        necessary: 'REQUIRED',
        valueType: 'POSITIVE_SHORT',
        key: 'key',
        defaultValue: null,
      },
    };
    const res = createBody(params);
    expect(res.key).toBeInstanceOf(Array);
    expect(res.key).toHaveLength(1);
    expect(res.key[0]).toBeInstanceOf(Function);
    expect(res.key[0](1)).toBeTruthy();
    expect(res.key[0](0)).toBeFalsy();
    expect(res.key[0](-1)).toBeFalsy();
  });

  it('expect POSITIVE_INT to be positiveNumber type', () => {
    const params = {
      key: {
        internal: false,
        necessary: 'REQUIRED',
        valueType: 'POSITIVE_INT',
        key: 'key',
        defaultValue: null,
      },
    };
    const res = createBody(params);
    expect(res.key).toBeInstanceOf(Array);
    expect(res.key).toHaveLength(1);
    expect(res.key[0]).toBeInstanceOf(Function);
    expect(res.key[0](1)).toBeTruthy();
    expect(res.key[0](0)).toBeFalsy();
    expect(res.key[0](-1)).toBeFalsy();
  });

  it('expect POSITIVE_LONG to be positiveNumber type', () => {
    const params = {
      key: {
        internal: false,
        necessary: 'REQUIRED',
        valueType: 'POSITIVE_LONG',
        key: 'key',
        defaultValue: null,
      },
    };
    const res = createBody(params);
    expect(res.key).toBeInstanceOf(Array);
    expect(res.key).toHaveLength(1);
    expect(res.key[0]).toBeInstanceOf(Function);
    expect(res.key[0](1000000000)).toBeTruthy();
    expect(res.key[0](0)).toBeFalsy();
    expect(res.key[0](-1)).toBeFalsy();
  });

  it('expect POSITIVE_DOUBLE to be positiveNumber type', () => {
    const params = {
      key: {
        internal: false,
        necessary: 'REQUIRED',
        valueType: 'POSITIVE_DOUBLE',
        key: 'key',
        defaultValue: null,
      },
    };
    const res = createBody(params);
    expect(res.key).toBeInstanceOf(Array);
    expect(res.key).toHaveLength(1);
    expect(res.key[0]).toBeInstanceOf(Function);
    expect(res.key[0](1.11)).toBeTruthy();
    expect(res.key[0](1)).toBeTruthy();
    expect(res.key[0](0)).toBeFalsy();
    expect(res.key[0](-1)).toBeFalsy();
  });
});
