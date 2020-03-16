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

import { string, option, array, object, number } from '../validation';
import { typeUtil } from '../apiUtils';

// TODO: will accomplish the rest test cases in #3124
describe('typeUtil() test cases', () => {
  it('normal case : body is as same as params', () => {
    const params = {
      group: 'groupx3obdcuzll',
      name: 'zkn8tknbd3pr',
      nodeNames: ['node2qasbg78bk'],
      tags: { name: 'zkn8tknbd3pr' },
    };
    const api = {
      group: [string],
      name: [string, option],
      nodeNames: [array],
      tags: [object, option],
    };
    const body = {
      group: 'groupx3obdcuzll',
      name: 'zkn8tknbd3pr',
      nodeNames: ['node2qasbg78bk'],
      tags: { name: 'zkn8tknbd3pr' },
    };

    const data = typeUtil([], { params, api, body });
    expect(params).toStrictEqual(data);
  });

  it('additional fields of body will not be overwrite', () => {
    const params = {
      group: 'groupx3obdcuzll',
      name: 'zkn8tknbd3pr',
      nodeNames: ['node2qasbg78bk'],
      tags: { name: 'zkn8tknbd3pr' },
    };
    const api = {
      group: [string],
      name: [string, option],
      nodeNames: [array],
      tags: [object, option],
    };
    const body = {
      foo: 'bar',
    };

    const data = typeUtil([], { params, api, body });
    expect(params).not.toStrictEqual(data);
    expect(body.foo).toBe(data.foo);
  });

  it('empty body is ok for type checking', () => {
    const params = {
      group: 'groupx3obdcuzll',
      name: 'zkn8tknbd3pr',
      nodeNames: ['node2qasbg78bk'],
      tags: { name: 'zkn8tknbd3pr' },
    };
    const api = {
      group: [string],
      name: [string, option],
      nodeNames: [array],
      tags: [object, option],
    };
    const body = {};

    const data = typeUtil([], { params, api, body });
    expect(params).toStrictEqual(data);
  });

  it('string value with number type will cause warnings', () => {
    const warns = [];
    const params = {
      group: 'group',
      name: 'name',
      nodeNames: ['node'],
      tags: { name: 'name' },
    };
    const api = {
      group: [number],
      name: [string, option],
      nodeNames: [array],
      tags: [object, option],
    };
    const body = {};

    const data = typeUtil(warns, { params, api, body });
    expect(warns).toHaveLength(1);
    expect(warns[0].message).toBe(
      `the field 'group' type is wrong. expected: 'number', actual value is: '"group"'`,
    );
    expect(params).not.toStrictEqual(data);
  });

  it('empty array value with object type will cause warnings', () => {
    const warns = [];
    const params = {
      group: 'group',
      name: 'name',
      nodeNames: [],
      tags: { name: 'name' },
    };
    const api = {
      group: [string],
      name: [string, option],
      nodeNames: [object],
      tags: [object, option],
    };
    const body = {};

    const data = typeUtil(warns, { params, api, body });
    expect(warns).toHaveLength(1);
    expect(warns[0].message).toBe(
      `the field 'nodeNames' type is wrong. expected: 'object', actual value is: '[]'`,
    );
    expect(params).not.toStrictEqual(data);
  });
});
