import axios from 'axios';

import { handleError } from '../utils/apiHelpers';
import * as _ from '../utils/helpers';

export const createPipeline = async params => {
  try {
    const res = await axios.post('/api/pipelines/create', params);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const updatePipeline = async ({ uuid, params }) => {
  try {
    const res = await axios.put(`/api/pipelines/update/${uuid}`, params);
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
    const res = await axios.post('/api/pipelines/query/rdb', params);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const createSource = async params => {
  try {
    const res = await axios.post('/api/sources/create', params);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const updateSource = async ({ uuid, params }) => {
  try {
    const res = await axios.put(`/api/sources/update/${uuid}`, params);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const fetchSources = async uuid => {
  try {
    const res = await axios.get(`/api/sources/${uuid}`);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const fetchPipelines = async uuid => {
  try {
    const res = await axios.get(`/api/pipelines/${uuid}`);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
