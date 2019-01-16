import axiosInstance from './axios';
import * as _ from 'utils/commonUtils';
import { handleError } from 'utils/apiUtils';

export const fetchZookeepers = async () => {
  try {
    const res = await axiosInstance.get(`/api/zookeepers`);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const createZookeeper = async params => {
  try {
    const url = `/api/zookeepers`;
    const data = {
      name: params.name,
      nodeNames: params.nodeNames || [],
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
