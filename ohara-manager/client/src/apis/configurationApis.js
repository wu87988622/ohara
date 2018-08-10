import axios from 'axios';

import { handleError } from '../utils/apiHelpers';
import { get } from '../utils/helpers';

export const fetchHdfs = async () => {
  try {
    const res = await axios.get('/api/configuration/hdfs');
    const isSuccess = get(res, 'data.isSuccess', false);

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
    const res = await axios.put('/api/configuration/validate/hdfs', {
      uri,
    });
    const isSuccess = get(res, 'data.isSuccess', false);

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
    const res = await axios.post('/api/configuration/save/hdfs', {
      name,
      uri,
    });
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
