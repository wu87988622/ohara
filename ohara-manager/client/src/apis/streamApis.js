import axiosInstance from './axios';

import { handleError } from 'utils/apiUtils';
import * as _ from 'utils/commonUtils';

export const fetchStreamJars = async () => {
  try {
    const res = await axiosInstance.get('/api/stream/jars');
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const createStreamJar = async params => {
  try {
    const { file } = params;
    const url = '/api/stream/jars';
    const formData = new FormData();
    formData.append('streamapp', file);
    const config = {
      headers: {
        'content-type': 'multipart/form-data',
      },
    };

    const res = await axiosInstance.post(url, formData, config);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const deleteStreamJar = async params => {
  try {
    const { uuid } = params;
    const url = `/api/stream/jars/${uuid}`;
    const res = await axiosInstance.delete(url);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const updateStreamJar = async params => {
  try {
    const { uuid, jarName } = params;
    const url = `/api/stream/jars/${uuid}`;
    const data = {
      jarName,
    };
    const res = await axiosInstance.put(url, data);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};