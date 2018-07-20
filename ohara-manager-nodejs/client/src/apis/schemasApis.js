import axios from 'axios';

import { handleError } from '../utils/apiHelpers';

export const fetchSchemas = async () => {
  try {
    return await axios.get('/api/schemas');
  } catch (err) {
    handleError(err);
  }
};

export const createSchemas = async params => {
  try {
    const res = await axios.post('/api/schemas', params);
    return res;
  } catch (err) {
    handleError(err);
  }
};
