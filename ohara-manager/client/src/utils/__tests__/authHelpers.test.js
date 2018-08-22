import localStorageMock from '../../__mocks__/localStorage';

import {
  setUserKey,
  deleteUserKey,
  getUserKey,
  isLoggedin,
} from '../authHelpers';

window.localStorage = localStorageMock;
const userKey = '123456abc';

describe('setUserKey', () => {
  it('does not set userKey when invoking with undefined', () => {
    setUserKey(undefined);
    expect(localStorage.userKey).toBe(undefined);
  });

  it('sets userKey to localstorage', () => {
    setUserKey(userKey);
    expect(localStorage.userKey).toBe(userKey);
  });
});

describe('isLoggedin', () => {
  it('returns true when userKey is exist', () => {
    const isUserLoggedin = isLoggedin();
    expect(isUserLoggedin).toBe(true);
  });
});

describe('getUserKey', () => {
  it('gets the userKey from localstorage', () => {
    const userKey = getUserKey();
    expect(userKey).toBe(userKey);
  });
});

describe('deleteUserKey', () => {
  it('deletes userKey', () => {
    deleteUserKey(userKey);
    expect(localStorage.userKey).toBe(undefined);
  });
});
