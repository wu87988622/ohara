import axios from 'axios';

import { handleError } from '../utils/apiHelpers';
import * as _ from '../utils/helpers';

export const savePipelines = async params => {
  try {
    const res = await axios.post('/api/pipelines/save', params);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const deletePipeline = async uuid => {
  try {
    const res = await axios.delete(`/api/pipelines/delete/${uuid}`);
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
    const res = await axios.put('/api/pipelines/validate/rdb', params);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const queryRdb = async params => {
  try {
    const res = await axios.get('/api/pipelines', params);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
