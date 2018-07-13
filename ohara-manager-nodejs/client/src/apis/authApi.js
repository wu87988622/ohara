import axios from 'axios';

import { handleError } from '../utils/apiHelpers';

export const login = async ({ username, password }) => {
  try {
    const res = await axios.post('/api/login', {
      username,
      password,
    });

    return res;
  } catch (err) {
    handleError(err);
  }
};
