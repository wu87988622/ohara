import axios from 'axios';

import { handleError } from '../utils/apiHelpers';

export const login = async ({ username, password }) => {
  try {
    const res = await axios.post('/api/login', {
      username,
      password,
    });

    if (!res.data.isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const logout = async () => {
  try {
    const res = await axios.get('/api/logout');

    if (!res.data.isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
