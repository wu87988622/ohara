import axios from 'axios';

import { handleError } from '../utils/apiHelpers';

export const fetchTopics = async () => {
  try {
    return await axios.get('/api/topics');
  } catch (err) {
    handleError(err);
  }
};
