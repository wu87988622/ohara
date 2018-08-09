import axios from 'axios';

import { handleError } from '../utils/apiHelpers';

export const validate = async params => {
  try {
    const res = await axios.put('/api/kafka/validate', params);

    if (!res.data.isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const save = async params => {
  try {
    const res = await axios.post('/api/kafka/save', params);

    if (!res.data.isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const fetchTopics = async () => {
  try {
    const res = await axios.get('/api/topics');
    if (!res.data.isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const createTopics = async params => {
  try {
    const res = await axios.post('/api/topics', params);
    if (!res.data.isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
