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

import { getCurrHdfsConnection, handleInputChange } from '../hdfsSinkUtils';

describe('getCurrHdfsConnection()', () => {
  it('gets the correct connection', () => {
    const connections = [{ name: 'a', id: '1234' }, { name: 'b', id: '5678' }];
    const targetId = '5678';

    const result = getCurrHdfsConnection(connections, targetId);
    expect(result).toEqual(connections[1]);
  });

  it('gets the default connection', () => {
    const connections = [{ name: 'a', id: '1234' }, { name: 'b', id: '5678' }];
    const targetId = '';

    const result = getCurrHdfsConnection(connections, targetId);
    expect(result).toEqual(connections[0]);
  });

  it('returns an empty object targetId is an empty string and connections is an empty array', () => {
    const connections = [];
    const targetId = '';
    const result = getCurrHdfsConnection(connections, targetId);
    expect(result).toEqual({});
  });
});

describe('handleInputChange()', () => {
  it('returns text input update', () => {
    const props = { updateHasChanges: jest.fn() };
    const state = {};
    const update = {
      name: 'abc',
      value: '123',
    };
    const result = handleInputChange(update)(state, props);

    expect(props.updateHasChanges).toHaveBeenCalledTimes(1);
    expect(props.updateHasChanges).toHaveBeenCalledWith(true);
    expect(result).toEqual({ abc: '123' });
  });

  it('returns checkbox input update', () => {
    const props = { updateHasChanges: jest.fn() };
    const state = {};
    const update = {
      name: 'abc',
      checked: true,
    };
    const result = handleInputChange(update)(state, props);

    expect(props.updateHasChanges).toHaveBeenCalledTimes(1);
    expect(props.updateHasChanges).toHaveBeenCalledWith(true);
    expect(result).toEqual({ abc: true });
  });
});
