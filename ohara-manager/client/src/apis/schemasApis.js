import axios from 'axios';

import { handleError } from 'utils/apiUtils';

export const fetchSchemas = async () => {
  try {
    return await axios.get('/api/schemas');
  } catch (err) {
    handleError(err);
  }
};
