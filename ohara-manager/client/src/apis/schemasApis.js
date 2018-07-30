import axios from 'axios';

import { handleError } from '../utils/apiHelpers';

export const fetchSchemas = async () => {
  try {
    return await axios.get('/api/schemas');
  } catch (err) {
    handleError(err);
  }
};

export const fetchSchemasDetails = async ({ uuid }) => {
  try {
    return await axios.get(`/api/schemas/${uuid}`);
  } catch (err) {
    handleError(err);
  }
};

export const createSchemas = async params => {
  try {
    return await axios.post('/api/schemas', params);
  } catch (err) {
    handleError(err);
  }
};
