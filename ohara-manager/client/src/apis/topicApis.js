import axios from 'axios';

import { handleError } from 'utils/apiUtils';
import * as _ from 'utils/commonUtils';

export const fetchTopic = async topicId => {
  try {
    const res = await axios.get(`/api/topics/${topicId}`);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
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
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
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
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
