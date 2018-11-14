import axios from 'axios';

import { handleError } from 'utils/apiUtils';
import * as _ from 'utils/commonUtils';

export const login = async ({ username, password }) => {
  try {
    const res = await axios.post('/api/login', {
      username,
      password,
    });
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
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
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
