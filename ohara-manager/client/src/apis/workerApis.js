import axiosInstance from './axios';
import { toNumber } from 'lodash';
import * as _ from 'utils/commonUtils';
import { handleError } from 'utils/apiUtils';

export const fetchWorkers = async () => {
  try {
    const res = await axiosInstance.get(`/api/workers`);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const fetchWorker = async name => {
  try {
    const res = await axiosInstance.get(`/api/workers/${name}`);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const createWorker = async params => {
  try {
    const url = `/api/workers`;
    const data = {
      name: params.name,
      clientPort: toNumber(params.clientPort),
      nodeNames: params.nodeNames || [],
      jars: params.plugins || [],
    };
    const config = {
      timeout: 3 * 60 * 1000, // set timeout to 3 minutes.
    };

    const res = await axiosInstance.post(url, data, config);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
