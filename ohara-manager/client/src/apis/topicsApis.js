import axios from 'axios';

import { handleError } from '../utils/apiHelpers';

export const fetchTopics = async () => {
  try {
    const res = await axios.get('/api/topics');
    if (!res.data.status) {
      handleError(res);
    }

    return res.data;
  } catch (err) {
    handleError(err);
  }
};

export const createTopics = async params => {
  try {
    const res = await axios.post('/api/topics', params);
    if (!res.data.status) {
      handleError(res);
    }

    return res.data;
  } catch (err) {
    handleError(err);
  }
};
