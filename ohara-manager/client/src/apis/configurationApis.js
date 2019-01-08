import axiosInstance from './axios';
import * as _ from 'utils/commonUtils';
import { handleError } from 'utils/apiUtils';

export const fetchHdfs = async () => {
  try {
    const res = await axiosInstance.get('/api/hdfs');
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const validateHdfs = async ({ uri }) => {
  try {
    const res = await axiosInstance.put('/api/validate/hdfs', {
      uri,
    });
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const saveHdfs = async ({ name, uri }) => {
  try {
    const res = await axiosInstance.post('/api/hdfs', {
      name,
      uri,
    });
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const deleteHdfs = async id => {
  try {
    const res = await axiosInstance.delete(`/api/hdfs/${id}`);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const fetchJdbc = async () => {
  try {
    const res = await axiosInstance.get('/api/jdbc');
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const validateRdb = async params => {
  try {
    const res = await axiosInstance.put('/api/validate/rdb', params);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const saveJdbc = async params => {
  try {
    const res = await axiosInstance.post('/api/jdbc', params);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const deleteJdbc = async id => {
  try {
    const res = await axiosInstance.delete(`/api/jdbc/${id}`);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
