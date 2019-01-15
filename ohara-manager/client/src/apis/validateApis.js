import { toNumber } from 'lodash';
import axiosInstance from './axios';
import * as _ from 'utils/commonUtils';
import { handleError } from 'utils/apiUtils';

export const validateNode = async ({ hostname, port, user, password }) => {
  try {
    const res = await axiosInstance.put('/api/validate/node', {
      hostname,
      port: toNumber(port),
      user,
      password,
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
