import axios from 'axios';

import { login, logout } from '../authApis';
import { handleError } from 'utils/apiUtils';

jest.mock('axios');
jest.mock('utils/apiUtils');

describe('authApis', () => {
  afterEach(jest.clearAllMocks);

  describe('login()', () => {
    it('handles success http call', async () => {
      const res = {
        data: {
          isSuccess: true,
        },
      };

      axios.post.mockImplementation(() => Promise.resolve(res));

      const params = { username: 'test', password: '1234' };

      const result = await login(params);
      expect(axios.post).toHaveBeenCalledTimes(1);
      expect(axios.post).toHaveBeenCalledWith('/api/login', params);
      expect(result).toBe(res);
    });

    it('handles success http call but with server error', async () => {
      const res = {
        data: {
          isSuccess: false,
        },
      };
      axios.post.mockImplementation(() => Promise.resolve(res));
      const params = { username: 'test', password: '1234' };

      const result = await login(params);

      expect(axios.post).toHaveBeenCalledTimes(1);
      expect(axios.post).toHaveBeenCalledWith('/api/login', params);
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
      axios.post.mockImplementation(() => Promise.reject(res));

      await login(params);
      expect(axios.post).toHaveBeenCalledTimes(1);
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

      axios.get.mockImplementation(() => Promise.resolve(res));

      const result = await logout();

      expect(axios.get).toHaveBeenCalledTimes(1);
      expect(axios.get).toHaveBeenCalledWith('/api/logout');
      expect(result).toBe(res);
    });

    it('handles success http call but with server error', async () => {
      const res = {
        data: {
          isSuccess: false,
        },
      };

      axios.get.mockImplementation(() => Promise.resolve(res));

      const result = await logout();

      expect(axios.get).toHaveBeenCalledTimes(1);
      expect(axios.get).toHaveBeenCalledWith('/api/logout');
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

      axios.get.mockImplementation(() => Promise.reject(res));

      await logout();
      expect(axios.get).toHaveBeenCalledTimes(1);
      expect(handleError).toHaveBeenCalledTimes(1);
      expect(handleError).toHaveBeenCalledWith(res);
    });
  });
});
