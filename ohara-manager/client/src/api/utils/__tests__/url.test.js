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

import { toQueryParameters } from '../url';

describe('toQueryParameters()', () => {
  it('returns "" if the params is not given', () => {
    expect(toQueryParameters()).toEqual('');
  });

  it('returns valid query parameters if the params is given', () => {
    const params = {
      a: 123,
      b: 'foo',
      bar: false,
    };
    const queryData = toQueryParameters(params);
    expect(queryData).toEqual('?a=123&b=foo&bar=false');
  });

  it('returns encoded query parameters for special parameters', () => {
    const params = {
      name: 'bar',
      group: 'my group',
      time: '2019-10-30 00:00:00',
    };
    const queryData = toQueryParameters(params);
    expect(queryData).toEqual(
      '?name=bar&group=my%20group&time=2019-10-30%2000%3A00%3A00',
    );
  });

  it('throw error if we cannot convert to query parameters', () => {
    const params = 'fake';
    expect(() => toQueryParameters(params)).toThrow(
      'you need to pass an object',
    );
  });
});
