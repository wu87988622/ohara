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

import localStorageMock from '__mocks__/localStorage';

import {
  setUserKey,
  deleteUserKey,
  getUserKey,
  isLoggedin,
} from '../authUtils';

window.localStorage = localStorageMock;
const userKey = '123456abc';

describe('setUserKey()', () => {
  it('does not set userKey when invoking with undefined', () => {
    setUserKey(undefined);
    expect(localStorage.userKey).toBe(undefined);
  });

  it('sets userKey to localstorage', () => {
    setUserKey(userKey);
    expect(localStorage.userKey).toBe(userKey);
  });
});

describe('isLoggedin()', () => {
  it('returns true when userKey is exist', () => {
    const isUserLoggedin = isLoggedin();
    expect(isUserLoggedin).toBe(true);
  });
});

describe('getUserKey()', () => {
  it('gets the userKey from localstorage', () => {
    const userKey = getUserKey();
    expect(userKey).toBe(userKey);
  });
});

describe('deleteUserKey()', () => {
  it('deletes userKey', () => {
    deleteUserKey(userKey);
    expect(localStorage.userKey).toBe(undefined);
  });
});
