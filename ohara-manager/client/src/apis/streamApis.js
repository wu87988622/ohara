import axiosInstance from './axios';

import { handleError } from 'utils/apiUtils';
import * as _ from 'utils/commonUtils';

export const fetchStreamJars = async pipelineId => {
  try {
    const res = await axiosInstance.get(`/api/stream/jars/${pipelineId}`);
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
    const { pipelineId, file } = params;
    const url = `/api/stream/jars/${pipelineId}`;
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
    const { id } = params;
    const url = `/api/stream/jars/${id}`;
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
    const { id, jarName } = params;
    const url = `/api/stream/jars/${id}`;
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
