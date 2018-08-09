import axios from 'axios';

import { handleError } from '../utils/apiHelpers';

export const validate = async ({ target, url }) => {
  try {
    const res = await axios.post('/api/configuration/validate', {
      target,
      url,
    });

    if (!res.data.isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const save = async ({ target, url }) => {
  try {
    const res = await axios.post('/api/configuration/save', {
      target,
      url,
    });

    if (!res.data.isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
