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

import { login, logout } from '../authApi';
import { handleError, axiosInstance } from '../apiUtils';

jest.mock('../apiUtils');

const loginUrl = '/api/login';
const logoutUrl = '/api/logout';

describe('authApi', () => {
  afterEach(jest.clearAllMocks);

  describe('login()', () => {
    it('handles success http call', async () => {
      const res = {
        data: {
          isSuccess: true,
        },
      };

      axiosInstance.post.mockImplementation(() => Promise.resolve(res));

      const params = { username: 'test', password: '1234' };

      const result = await login(params);
      expect(axiosInstance.post).toHaveBeenCalledTimes(1);
      expect(axiosInstance.post).toHaveBeenCalledWith(loginUrl, params);
      expect(result).toBe(res);
    });

    it('handles success http call but with server error', async () => {
      const res = {
        data: {
          isSuccess: false,
        },
      };
      axiosInstance.post.mockImplementation(() => Promise.resolve(res));
      const params = { username: 'test', password: '1234' };

      const result = await login(params);

      expect(axiosInstance.post).toHaveBeenCalledTimes(1);
      expect(axiosInstance.post).toHaveBeenCalledWith(loginUrl, params);
      expect(handleError).toHaveBeenCalledTimes(1);
      expect(handleError).toHaveBeenCalledWith(result);
    });

    it('handles failed http call', async () => {
      const res = {
        data: {
          errorMessage: {
            message: 'error!',
          },
        },
      };

      const params = { username: 'test', password: '1234' };
      axiosInstance.post.mockImplementation(() => Promise.reject(res));

      await login(params);
      expect(axiosInstance.post).toHaveBeenCalledTimes(1);
      expect(handleError).toHaveBeenCalledTimes(1);
      expect(handleError).toHaveBeenCalledWith(res);
    });
  });

  describe('logout()', () => {
    it('handle success http call', async () => {
      const res = {
        data: {
          isSuccess: true,
        },
      };

      axiosInstance.get.mockImplementation(() => Promise.resolve(res));

      const result = await logout();

      expect(axiosInstance.get).toHaveBeenCalledTimes(1);
      expect(axiosInstance.get).toHaveBeenCalledWith(logoutUrl);
      expect(result).toBe(res);
    });

    it('handles success http call but with server error', async () => {
      const res = {
        data: {
          isSuccess: false,
        },
      };

      axiosInstance.get.mockImplementation(() => Promise.resolve(res));

      const result = await logout();

      expect(axiosInstance.get).toHaveBeenCalledTimes(1);
      expect(axiosInstance.get).toHaveBeenCalledWith(logoutUrl);
      expect(handleError).toHaveBeenCalledTimes(1);
      expect(handleError).toHaveBeenCalledWith(result);
    });

    it('handles failed http call', async () => {
      const res = {
        data: {
          errorMessage: {
            message: 'error!',
          },
        },
      };

      axiosInstance.get.mockImplementation(() => Promise.reject(res));

      await logout();
      expect(axiosInstance.get).toHaveBeenCalledTimes(1);
      expect(handleError).toHaveBeenCalledTimes(1);
      expect(handleError).toHaveBeenCalledWith(res);
    });
  });
});
