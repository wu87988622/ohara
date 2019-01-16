import { toNumber } from 'lodash';
import axiosInstance from './axios';
import * as _ from 'utils/commonUtils';
import { handleError } from 'utils/apiUtils';

export const fetchTopic = async topicId => {
  try {
    const res = await axiosInstance.get(`/api/topics/${topicId}`);
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
    const res = await axiosInstance.get('/api/topics');
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const createTopic = async params => {
  const { name, numberOfPartitions, numberOfReplications } = params;
  try {
    const data = {
      name,
      numberOfPartitions: toNumber(numberOfPartitions),
      numberOfReplications: toNumber(numberOfReplications),
    };
    const res = await axiosInstance.post('/api/topics', data);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
